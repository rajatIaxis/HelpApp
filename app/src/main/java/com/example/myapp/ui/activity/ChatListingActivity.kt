package com.example.myapp.ui.activity

import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.myapp.R
import com.example.myapp.databinding.ActivityChatListingBinding
import com.example.myapp.db.AppDatabase
import com.example.myapp.db.DatabaseUtil
import com.example.myapp.db.entity.ChatEntity
import com.example.myapp.ui.adapter.MessageAdapter
import com.example.myapp.ui.directmessage.DirectMessageFragment
import com.example.myapp.utils.AppUtils
import com.example.myapp.utils.Constants
import com.example.myapp.utils.NPALinearLayoutManager
import java.util.*

/**
 * Using LiveData to keep the UI updated with the data changes in the database.
 */
data class ChatMessage(var name: String, var message: String, var icon: Int = R.drawable.contact)

class ChatListingActivity : AppCompatActivity() {
    var binding: ActivityChatListingBinding? = null
    var adapter: MessageAdapter? = null
    var mChatList: MutableList<ChatEntity>? = null
    var appDatabase: AppDatabase? = null
    private var recyclerView: RecyclerView? = null
    private var mObservableChats: LiveData<List<ChatEntity>>? = null
    private var layoutManager: NPALinearLayoutManager? = null
    private var receiverMessageFlag = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat_listing)
        appDatabase = AppDatabase.getDatabase(this.application)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = intent.getStringExtra("contactName")

        recyclerView = findViewById(R.id.recyclerview_message_view)
        initRecyclerView()
        chatHistory
        clickListeners()
    }

    private fun clickListeners() {
        binding!!.buttonChatSend.setOnClickListener(View.OnClickListener { v: View? ->
            // adding the sender and the receiver data
            if (!binding!!.editTextChat.text.toString().trim { it <= ' ' }.equals(
                    "",
                    ignoreCase = true
                )
            ) {
                addSenderMessage()
                // clear edit text
                binding!!.editTextChat.setText("")
            } else {
                Toast.makeText(this@ChatListingActivity,"Please enter some message",Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun addSenderMessage() {
        val chatEntitySender = ChatEntity()
        chatEntitySender.chatType = Constants.MESSAGE_SENDER
        chatEntitySender.chatContent = binding!!.editTextChat.text.toString()
        chatEntitySender.date = Date()
        chatEntitySender.sender = "You"
        chatEntitySender.receiver = intent.getStringExtra("contactName")
        mChatList!!.add(chatEntitySender)
        receiverMessageFlag = true
        DatabaseUtil.addSenderChatToDataBase(appDatabase, chatEntitySender)
    }

    private fun addReceiverMessage() {
        Handler().postDelayed({
            val chatEntityReceiver = ChatEntity()
            chatEntityReceiver.chatType = Constants.MESSAGE_RECEIVER
            chatEntityReceiver.chatContent = DatabaseUtil.getDirectChat()
            chatEntityReceiver.date = Date()
            chatEntityReceiver.sender = intent.getStringExtra("contactName")
            chatEntityReceiver.receiver = "You"
            mChatList!!.add(chatEntityReceiver)
            receiverMessageFlag = false
            DatabaseUtil.addReceiverChatToDataBase(appDatabase, chatEntityReceiver)
        }, 1000)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return true
    }

    private fun initRecyclerView() {
        layoutManager = NPALinearLayoutManager(this)
        val itemAnimator: SimpleItemAnimator = DefaultItemAnimator()
        itemAnimator.supportsChangeAnimations = false
        recyclerView?.setHasFixedSize(true)
        recyclerView?.layoutManager = layoutManager
        recyclerView?.itemAnimator = itemAnimator
        recyclerView?.setItemViewCacheSize(20)
        recyclerView?.isDrawingCacheEnabled = true
        recyclerView?.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
        mChatList = ArrayList()
        adapter = MessageAdapter(this@ChatListingActivity, mChatList)
        recyclerView?.adapter = adapter
    }

    private val chatHistory: Unit
        private get() {
//            mObservableChats = appDatabase!!.chatDao().loadAllChatHistory()
            mObservableChats = appDatabase!!.chatDao().loadAllChatHistoryByUser(intent.getStringExtra("contactName"))

            mObservableChats?.observe(
                this,
                Observer<MutableList<ChatEntity>?> { chatsHistoryList ->
                    if (chatsHistoryList != null) {
                        mChatList = chatsHistoryList
                        adapter!!.refresh(mChatList)
                        adapter!!.notifyItemInserted(chatsHistoryList.size - 1)
                        if (chatsHistoryList.size > 0) layoutManager!!.scrollToPosition(
                            chatsHistoryList.size - 1
                        )
                        if (receiverMessageFlag) {
                            addReceiverMessage()
                        }
                    }
                })
        }
    companion object{

    }
}