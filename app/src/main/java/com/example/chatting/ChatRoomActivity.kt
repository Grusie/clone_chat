package com.example.chatting


import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatting.Model.Messages
import com.example.chatting.Model.UserRoom
import com.example.chatting.Model.chatRoomUser
import com.example.chatting.databinding.ActivityChatRoomBinding
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import java.io.BufferedReader
import java.io.File
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.lang.Exception
import java.text.SimpleDateFormat
import kotlin.math.log

class ChatRoomActivity : AppCompatActivity() {
    lateinit var userName: String
    lateinit var userEmail: String
    var chatRoomId: String? = null
    private val database = Firebase.database
    private val messageRef = database.getReference("Messages")
    private val userRoomRef = database.getReference("UserRoom")
    private val chatRoomUserRef = database.getReference("chatRoomUser")
    private val userStatusRef = database.getReference("UserStatus")

    private val messageList = mutableListOf<Messages>()
    lateinit var adapter: ChatRoomAdatpter
    lateinit var binding: ActivityChatRoomBinding
    lateinit var myRecyclerView: RecyclerView
    var lastDate:Long ?= 0
    var currentDate:Long ?= 0
    var loadMsg : Messages ?= null
    var myUser = ""

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.chatroomToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        try { userEmail = intent.getSerializableExtra("userEmail") as String } catch (e: Exception) { }
        try { chatRoomId = intent.getStringExtra("chatRoomId") as String } catch (e: Exception) { }
        try {
            userName = intent.getStringExtra("userName") as String
            binding.chatroomToolbar.setTitle(userName)
        } catch (e: Exception) {
        }

        adapter = ChatRoomAdatpter(messageList, chatRoomId!!)

        binding.rvChatroom.adapter = adapter
        binding.rvChatroom.layoutManager = LinearLayoutManager(this)
        myRecyclerView = binding.rvChatroom

        adapter.notifyDataSetChanged()
        loadChatData()

        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                if (binding.etMessage.length() > 0) {
                    binding.btnSend.setBackgroundColor(applicationContext.resources.getColor(R.color.yellow))
                    binding.btnSend.isEnabled = true
                } else {
                    binding.btnSend.setBackgroundColor(Color.WHITE)
                    binding.btnSend.isEnabled = false
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (binding.etMessage.length() > 0) {
                    binding.btnSend.setBackgroundColor(applicationContext.resources.getColor(R.color.yellow))
                    binding.btnSend.isEnabled = true
                } else {
                    binding.btnSend.setBackgroundColor(Color.WHITE)
                    binding.btnSend.isEnabled = false
                }
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (binding.etMessage.length() > 0) {
                    binding.btnSend.setBackgroundColor(applicationContext.resources.getColor(R.color.yellow))
                    binding.btnSend.isEnabled = true
                } else {
                    binding.btnSend.setBackgroundColor(Color.WHITE)
                    binding.btnSend.isEnabled = false
                }
            }
        })

        myRecyclerView.addOnLayoutChangeListener { view, i, i2, i3, bottom, i5, i6, i7, oldBottom ->
            if(bottom < oldBottom)
                binding.rvChatroom.scrollBy(0, oldBottom - bottom)
        }

        //????????? ????????? ?????? ?????????
        binding.btnSend.setOnClickListener {
            val msg = binding.etMessage.text.toString()   //msg
            val time = System.currentTimeMillis()
            if (msg != "") {
                binding.etMessage.text.clear()

                val messageData = Messages(
                    message = msg,
                    timestamp = time,
                    sender = MyApplication.auth.currentUser?.email.toString(),
                    read = false
                )
                val userRoom = UserRoom(
                    lastmessage = msg,
                    timestamp = time,
                    sender = MyApplication.auth.currentUser?.email.toString()
                )

                val key = messageRef.child("$chatRoomId").push().key

                chatRoomUserRef.child(chatRoomId!!)
                    .get()
                    .addOnSuccessListener {
                        val userList = mutableListOf<String>()
                        for (user in it.children){
                            //????????? ?????? ?????? ????????? In Out ????????? ?????? ??? read = true/false ??????
                                // -> ???????????? ???????????? if ?????? ?????? X
                            if(user.value != MyApplication.auth.currentUser?.email){
                                userStatusRef.child(chatRoomId!!).child(user.key!!)
                                    .get()
                                    .addOnSuccessListener { userStatus ->
                                        if(userStatus.value == "In"){
                                            val msg = messageData.copy(read = true)
                                            messageRef.child("$chatRoomId").child(key!!).setValue(msg)
                                        } else {
                                            messageRef.child("$chatRoomId").child(key!!).setValue(messageData)
                                            val intent = Intent(this, MessagingControl::class.java)
                                            val token = getToken("${user.value}")
                                            /*intent.putExtra("token", token)
                                            intent.putExtra("chatRoomId", chatRoomId)
                                            intent.putExtra("LastMessage", msg)
                                            intent.putExtra("sender", MyApplication.auth.currentUser?.email)
                                            startActivity(intent)*/
                                        }

                                        userRoomRef.child("$chatRoomId").setValue(userRoom)
                                        adapter.notifyDataSetChanged()
                                        currentDate = messageData.timestamp
                                        myRecyclerView.scrollToPosition(adapter.itemCount-1)
                                    }
                            }
                            userList.add(user.value as String)
                        }

                        if(userList[0] == userList[1]){
                            val msg = messageData.copy(read = true)
                            messageRef.child("$chatRoomId").child(key!!).setValue(msg)
                            userRoomRef.child("$chatRoomId").setValue(userRoom)
                            adapter.notifyDataSetChanged()
                            currentDate = messageData.timestamp
                            myRecyclerView.scrollToPosition(adapter.itemCount-1)
                        }
                    }
            }
        }
    }

    private fun getToken(user: String): String{
        var token : String = ""/*
        MyApplication.db.collection("profile").document("$user").get()
            .addOnSuccessListener {
                    fields -> for (field in fields) token = field["token"]
            }*/
        return token
    }

    private fun loadChatData() {
        val messagesDataListener = object : ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                lastDate = loadMsg?.timestamp
                loadMsg = snapshot.getValue<Messages>()!!
                currentDate = loadMsg?.timestamp
                dateCalc()
                messageList.add(loadMsg!!)
                adapter.notifyDataSetChanged()
                myRecyclerView.scrollToPosition(adapter.itemCount-1)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val newMsgList = mutableListOf<Messages>()
                for(message in messageList){
                    val msg = message.copy(read = true)
                    newMsgList.add(msg)
                }
                messageList.clear()
                messageList.addAll(newMsgList)
                adapter.notifyDataSetChanged()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                snapshot.getValue<Messages>()?.let { adapter.removeItem(it) }
                if(messageList[messageList.size-1].sender == ""){
                    messageList.removeAt(messageList.size-1)
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }
        messageRef.child("$chatRoomId").addChildEventListener(messagesDataListener)

    }

    //???????????? ?????? ???????????? ?????? ??????
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_chat_room, menu)
        return true
    }

    //???????????? ?????? ?????? ???
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item?.itemId){
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.chatting_menu_exit -> {
                AlertDialog.Builder(this).apply {
                    setTitle("??????")
                    setMessage("???????????? ??????????????????????")
                    setPositiveButton(
                        "OK",
                        DialogInterface.OnClickListener { dialog, which ->
                            MyApplication.realtime.child("chatRoomUser")
                                .child(chatRoomId.toString())
                                .removeValue()

                            MyApplication.realtime.child("UserRoom")
                                .child(chatRoomId.toString())
                                .removeValue()

                            MyApplication.realtime.child("Messages")
                                .child(chatRoomId.toString())
                                .removeValue()
                            finish()
                        })
                    setNegativeButton(
                        "Cancel",
                        DialogInterface.OnClickListener { dialog, which ->
                            dialog.dismiss()
                        })
                    show()
                }
                return super.onOptionsItemSelected(item)
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    fun dateCalc() {
        if(lastDate != null && currentDate !=null) {
            if(SimpleDateFormat("yyyy??? MM??? dd???").format(lastDate) < SimpleDateFormat("yyyy??? MM??? dd???").format(currentDate)) {
                lastDate = currentDate
                addDate()
            }
        }
        else if(lastDate == null){
            addDate()
        }
    }

    fun addDate() {
        messageList.add(
            Messages(
                message = "",
                timestamp = currentDate!!,
                sender = ""
            ))
    }

    override fun onResume() {
        super.onResume()
        setUserStatus("In")
        setMessagesRead()
    }

    override fun onStop() {
        super.onStop()
        setUserStatus("Out")
    }

    private fun setMessagesRead() {
        val keys = mutableListOf<String>()
        messageRef.child(chatRoomId!!)
            .get()
            .addOnSuccessListener { messages ->
                for (message in messages.children){
                    keys.add(message.key!!)
                }
                for (key in keys){
                    messageRef.child(chatRoomId!!).child(key).child("sender")
                        .get()
                        .addOnSuccessListener {
                            if(it.value != MyApplication.auth.currentUser?.email){
                                messageRef.child(chatRoomId!!).child(key).child("read").setValue(true)
                            }
                        }
                }
            }
    }

    private fun setUserStatus(userStatus: String) {
        chatRoomUserRef.child(chatRoomId!!)
            .get()
            .addOnSuccessListener {
                for (user in it.children){
                    if (user.value == MyApplication.auth.currentUser?.email){
                        myUser = user.key as String
                        userStatusRef.child(chatRoomId!!).child(user.key!!).setValue(userStatus)
                    }
                }
            }
    }
}

