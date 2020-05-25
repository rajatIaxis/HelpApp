package com.example.myapp.ui.ledger

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myapp.R
import com.example.myapp.databinding.ActivityChatListingBinding.inflate
import com.example.myapp.databinding.FragmentDirectmessageBinding
import com.example.myapp.databinding.FragmentLedgerBinding
import com.example.myapp.db.AppDatabase
import com.example.myapp.db.DatabaseUtil
import com.example.myapp.db.entity.GroupChatEntity
import com.example.myapp.db.entity.LedgerEntity
import com.example.myapp.ui.adapter.GroupMessageAdapter
import com.example.myapp.ui.adapter.LedgerAdapter
import com.example.myapp.ui.main.Model
import com.example.myapp.ui.main.MyAdapter
import com.example.myapp.utils.NPALinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*
import kotlin.collections.ArrayList

class LedgerFragment : Fragment() {

    private lateinit var notificationsViewModel: NotificationsViewModel
    var list = mutableListOf<Model>()
    private lateinit var listView: ListView
    private lateinit var listAdapter: MyAdapter
    private lateinit var root: View
    private lateinit var requiredItems:ArrayList<String>
    var binding: FragmentLedgerBinding? = null
    var appDatabase: AppDatabase? = null

    var ledgerAdapter: LedgerAdapter? = null
    var mLedgerList: MutableList<LedgerEntity>? = null
    private var mObservableChats: LiveData<List<LedgerEntity>>? = null
    private var layoutManager: NPALinearLayoutManager? = null
    private lateinit var pullToRefresh: SwipeRefreshLayout

    init{
        list.add(Model("Yelahanka", "Satellite bus station", img = R.drawable.helpwe))
        list.add(Model("SVIT", "Canteen"))
        list.add(Model("SVIT", "Boys Hostel"))

        Log.d("com.example.myapp.ui.ledger.LedgerFragment", "Init")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        notificationsViewModel =
            ViewModelProviders.of(this).get(NotificationsViewModel::class.java)
        root = inflater.inflate(R.layout.fragment_ledger, container, false)
        listView = root.findViewById(R.id.listView)
        listView.adapter = MyAdapter(root.context, R.layout.row, list)
        mLedgerList = ArrayList()

        appDatabase = AppDatabase.getDatabase(activity?.application)

        pullToRefresh = root.findViewById(R.id.swiperefresh)
        pullToRefresh.setOnRefreshListener {refresh()
        }

        listView.setOnItemClickListener { parent: AdapterView<*>, view: View, position:Int, id:Long ->
            Toast.makeText(root.context, "Clicked on" + list[position].landmarkName, Toast.LENGTH_LONG).show()
            val builder = AlertDialog.Builder(context)
            builder.setTitle(Html.fromHtml("<font size = '18'><b>Help!</b>"))

            val message = StringBuilder()
            if(requiredItems.isNotEmpty()) {
                message.append("I am in great need of:").append("\n\n")
                for (item in requiredItems) {
                    message.append("     ○  ").append(item).append("\n\n")
                }
                message.delete(message.length - 2, message.length)

                builder.setMessage(message.toString())
            }
            else
            {
                builder.setMessage("")
            }
            builder.setPositiveButton("Show location") { _, id ->
                val latitude = list[position].latLongAcc[0].toDouble()
                val longitude = list[position].latLongAcc[1].toDouble()
                val gmmIntentUri: Uri = Uri.parse("geo:%f,%f?q=%f,%f".format(latitude, longitude, latitude, longitude)) //first 2 for map_view, next 2 for dropping pin
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                startActivity(mapIntent)
            }

            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
             builder.create().show()
        }
        val add = root.findViewById<FloatingActionButton>(R.id.add)
        add.setOnClickListener{
            val intent = Intent(root.context, TakeInput::class.java)
            intent.putExtra("key", "value")
            startActivityForResult(intent, 6969)
        }

        return root

    }

    private fun refresh ()
    {
        //clears the existing list and then fetches from db and updates the list
        //right now only current db entries show up
        //merging db between devices might be the solution
        var i = 0
        while(i< mLedgerList?.size!!)
        {
            val fetchedData = mLedgerList!![i]

            val fetchedLocation = fetchedData.location
            val fetchedLandmark = fetchedData.landmark
            list.add(Model(fetchedLocation,fetchedLandmark))
            i++
        }
        listView.adapter = MyAdapter(root.context, R.layout.row, list)

        Toast.makeText(root.context, "Updated", Toast.LENGTH_LONG).show()
        pullToRefresh.isRefreshing = false;
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 6969){
            if(resultCode == 7070){
                val ledgerEntity = LedgerEntity()
                val locationName = data!!.getStringExtra("Location")
                val landmark = data!!.getStringExtra("Landmark")
                val latLongAcc= data!!.getStringArrayListExtra("LatLongAcc")
                requiredItems = data!!.getStringArrayListExtra("CheckedItems")
                ledgerEntity.location = locationName
                ledgerEntity.landmark = landmark
                ledgerEntity.needs = requiredItems.toString()
                ledgerEntity.date = Date() // date is added here
                ledgerEntity.sender = "You"
                Log.d("com.example.myapp.ui.ledger.LedgerFragment-onActivityResult", "location = $locationName")
                Log.d("com.example.myapp.ui.ledger.LedgerFragment-onActivityResult", "landmark = $landmark")
                Log.d("com.example.myapp.ui.ledger.LedgerFragment-onActivityResult", "landmark = $latLongAcc")
                DatabaseUtil.addNewLedgerToDataBase(appDatabase,ledgerEntity) //entry into db here
                list.add(Model(locationName, landmark, latLongAcc, requiredItems))
                listView.adapter = MyAdapter(root.context, R.layout.row, list)
            }
        }
    }


}