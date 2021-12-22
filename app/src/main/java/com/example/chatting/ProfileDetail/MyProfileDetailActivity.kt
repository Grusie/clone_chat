package com.example.chatting.ProfileDetail

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.chatting.ChatRoomActivity
import com.example.chatting.Model.UserData
import com.example.chatting.Model.chatRoomUser
import com.example.chatting.MyApplication
import com.example.chatting.R
import com.example.chatting.databinding.ActivityMyProfileDetailBinding
import com.example.chatting.util.URIPathHelper
import com.example.chatting.util.myCheckPermission
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.io.File
import java.lang.Exception

class MyProfileDetailActivity : AppCompatActivity() {

    private lateinit var userData: UserData
    private lateinit var filename: String
    val chatRoomRef = Firebase.database.getReference("chatRoomUser")
    var chatRoomId: String? = null
    var user1: String? = null
    var user2: String? = null
    private lateinit var galleryIntent: ActivityResultLauncher<Intent>
    private var profileImgFilePath: String? = null
    private var backgroundImgFilePath: String? = null

    val binding by lazy { ActivityMyProfileDetailBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        //리사이클러 뷰 항목 클릭시 넘어온 userData 정보를 화면 뷰에 구성
        userData = intent.getParcelableExtra<UserData>("userData")!!
        Log.d("test", userData.toString())
        editState(checkProfileUser())
        binding()

        //취소 버튼 클릭 시
        binding.myProfileBack.setOnClickListener { finish() }
        binding.myProfileBackEdit.setOnClickListener {
            editState(checkProfileUser())
        }

        //갤러리 인텐트
        galleryIntent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){

            if(it.resultCode == RESULT_OK){

                val cursor = contentResolver.query(
                    it.data!!.data as Uri,
                    arrayOf(MediaStore.Images.Media.DATA), null, null, null
                )

                //val uriPathHelper = URIPathHelper()

                when(filename){
                    "profile" -> {
                        Glide
                            .with(this)
                            .load(it.data!!.data)
                            .apply(RequestOptions().override(150, 150))
                            .centerCrop()
                            .into(binding.myProfileImage)

                        cursor?.moveToFirst().let{
                            profileImgFilePath = cursor?.getString(0) as String
                        }

                        //profileImgFilePath = uriPathHelper.getPath(this, it.data!!.data!!)

                        Log.d("test-galleryintent", profileImgFilePath!!)
                    }

                    "background" -> {
                        Glide
                            .with(this)
                            .load(it.data!!.data)
                            .centerCrop()
                            .into(binding.myProfileBackEdit)

                        cursor?.moveToFirst().let{
                            backgroundImgFilePath = cursor?.getString(0) as String
                        }

                        Log.d("Test", backgroundImgFilePath!!)
                    }
                }
            }
        }


        //프로필 사진 클릭 시
        binding.myProfileImage.setOnClickListener {
            launchGalleryApp("profile")
        }

        //배경 사진 편집 클릭 시
        binding.myProfileEditCamera.setOnClickListener {
            launchGalleryApp("background")
        }


        //채팅 버튼 클릭 시
        binding.myProfileChat.setOnClickListener {
            try {
                createChatRoom()
            } catch (e: Exception) {
                Log.d("grusie", "$e")
            }
        }

        //편집 버튼 클릭 시
        binding.myProfileEdit.setOnClickListener {
            editState("edit")
        }

        //저장 버튼 클릭 시
        binding.myProfileSaveEdit.setOnClickListener {

            val newName = binding.myProfileNameEdit.text.toString()
            val newStatusMsg = binding.myProfileStatusMsgEdit.text.toString()
            val newProfileMusic = binding.myProfileMusicEdit.text.toString()

            if (newName.isEmpty()) {
                Toast.makeText(this, "이름을 다시 설정해주세요.", Toast.LENGTH_SHORT).show()
            } else {

                saveImages()
                //setImages()

                updateAndGetValue("name", newName)
                updateAndGetValue("statusMsg", newStatusMsg)
                updateAndGetValue("profileMusic", newProfileMusic)

                editState(checkProfileUser())
            }
        }
    }

    private fun setImages() {

        val imgRefProfile =
            MyApplication.storage.reference.child("${userData.email}/profile")
        Log.d("test-setImages", "$imgRefProfile")

        Glide
            .with(this@MyProfileDetailActivity)
            .load(imgRefProfile)
            .error(R.drawable.img_profile)
            .into(binding.myProfileImage)

        val imgRefBackground =
            MyApplication.storage.reference.child("${userData.email}/background")

        Glide
            .with(this@MyProfileDetailActivity)
            .load(imgRefBackground)
            .into(binding.myProfileBackgroundImg)
    }

    private fun saveImages() {
        if(profileImgFilePath != null){
            saveStorage("profile", profileImgFilePath!!)
        }

        if(backgroundImgFilePath != null){
            saveStorage("background", backgroundImgFilePath!!)
        }
    }

    private fun launchGalleryApp(imageKind: String) {

        myCheckPermission(this)

        if(ContextCompat.checkSelfPermission(
                this@MyProfileDetailActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED){

                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    intent.type = "image/*"
                    filename = imageKind
                    galleryIntent.launch(intent)

        }
    }

    private fun binding() {
            binding.run {
                myProfileName.text = userData.name
                myProfileStatusMsg.text = userData.statusMsg
                myProfileMusic.text = userData.profileMusic

                setImages()
            }
        }

        private fun checkProfileUser(): String =
            //내 프로필인 경우 vs 내 프로필이 아닌 경우
            if (userData.email == MyApplication.auth.currentUser?.email) {
                "myProfile"
            } else {
                "notMyProfile"
            }

        private fun updateAndGetValue(field: String, newValue: String) {

            MyApplication.db.collection("profile")
                .document(userData.email)
                .update(field, newValue)
                .addOnSuccessListener {
                    //update 성공 시 newValue get -> userData에 삽입
                    MyApplication.db.collection("profile")
                        .document(userData.email)
                        .get()
                        .addOnSuccessListener { document ->
                            when (field) {
                                "name" -> {
                                    userData.name = document[field] as String
                                }
                                "statusMsg" -> {
                                    userData.statusMsg = document[field] as String
                                }
                                "profileMusic" -> {
                                    userData.profileMusic = document[field] as String
                                }
                            }

                            binding()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "설정 실패", Toast.LENGTH_SHORT).show()
                        }
                }
        }

        private fun editState(state: String) {
            when (state) {
                "edit" -> {
                    binding.run {
                        myProfileChat.visibility = View.GONE
                        myProfileEdit.visibility = View.GONE
                        myProfileBack.visibility = View.GONE
                        myProfileFavorites.visibility = View.GONE
                        myProfileName.visibility = View.INVISIBLE
                        myProfileStatusMsg.visibility = View.INVISIBLE
                        myProfileMusic.visibility = View.GONE

                        myProfileNameEdit.visibility = View.VISIBLE
                        myProfileStatusMsgEdit.visibility = View.VISIBLE
                        myProfileBackEdit.visibility = View.VISIBLE
                        myProfileSaveEdit.visibility = View.VISIBLE
                        myProfileMusicEdit.visibility = View.VISIBLE
                        myProfileEditCamera.visibility = View.VISIBLE

                        myProfileImage.isEnabled = true
                    }

                    val hintName = binding.myProfileName.text
                    val hintStatusMsg = binding.myProfileStatusMsg.text
                    val hintProfileMusic = binding.myProfileMusic.text

                    binding.myProfileNameEdit.setText(hintName)
                    binding.myProfileStatusMsgEdit.setText(hintStatusMsg)
                    binding.myProfileMusicEdit.setText(hintProfileMusic)



                }

                "myProfile" -> {
                    binding.run {
                        myProfileChat.visibility = View.VISIBLE
                        myProfileEdit.visibility = View.VISIBLE
                        myProfileBack.visibility = View.VISIBLE
                        myProfileFavorites.visibility = View.VISIBLE
                        myProfileName.visibility = View.VISIBLE
                        myProfileStatusMsg.visibility = View.VISIBLE
                        myProfileMusic.visibility = View.VISIBLE

                        myProfileNameEdit.visibility = View.GONE
                        myProfileStatusMsgEdit.visibility = View.GONE
                        myProfileBackEdit.visibility = View.GONE
                        myProfileSaveEdit.visibility = View.GONE
                        myProfileMusicEdit.visibility = View.GONE
                        myProfileEditCamera.visibility = View.GONE

                        myProfileImage.isEnabled = false
                    }
                }

                "notMyProfile" -> {
                    binding.run {
                        myProfileChat.visibility = View.VISIBLE
                        myProfileEdit.visibility = View.GONE
                        myProfileBack.visibility = View.VISIBLE
                        myProfileFavorites.visibility = View.VISIBLE
                        myProfileName.visibility = View.VISIBLE
                        myProfileStatusMsg.visibility = View.VISIBLE
                        myProfileMusic.visibility = View.VISIBLE

                        myProfileNameEdit.visibility = View.GONE
                        myProfileStatusMsgEdit.visibility = View.GONE
                        myProfileBackEdit.visibility = View.GONE
                        myProfileSaveEdit.visibility = View.GONE
                        myProfileMusicEdit.visibility = View.GONE
                        myProfileEditCamera.visibility = View.GONE

                        myProfileImage.isEnabled = false
                    }
                }
            }
        }

        private fun saveStorage(imageKind: String, filePath: String) {
            val storage = MyApplication.storage
            val storageRef = storage.reference
            val imgRef =
                storageRef.child("${MyApplication.auth.currentUser?.email}/${imageKind}")
            Log.d("test-saveStorage", "$imgRef")
            val file = Uri.fromFile(File(filePath))

            imgRef
                .putFile(file)
                .addOnSuccessListener {
                    Toast.makeText(this, "사진이 저장되었습니다.", Toast.LENGTH_SHORT).show()

                    setImages()
                }
                .addOnFailureListener {
                    Log.d("grusie", "error : $it")
                }
        }

        private fun openChatRoom(context: Context) {
            val intent = Intent(context, ChatRoomActivity::class.java)
            intent.putExtra("userName", binding.myProfileName.text.toString())
            intent.putExtra("userEmail", userData.email)
            intent.putExtra("chatRoomId", chatRoomId)
            startActivity(intent)
        }

        private fun createChatRoom() {
            val valueListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (chatRoomInfo in snapshot.children) {
                        chatRoomId = null
                        user1 = null
                        user2 = null
                        for (chatRoomUserData in chatRoomInfo.children) {
                            if (chatRoomUserData.value == MyApplication.auth.currentUser?.email) {
                                user1 = chatRoomUserData.value as String
                            } else if (chatRoomUserData.value == userData.email) {
                                user2 = chatRoomUserData.value as String
                            }
                        }
                        if (user1 != null && user2 != null) {
                            chatRoomId = chatRoomInfo.key
                            break
                        }
                    }
                    if (chatRoomId == null) {
                        createChatRoomUser()
                    }
                    else openChatRoom(this@MyProfileDetailActivity)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("grusie", "failed")
                }
            }
            chatRoomRef.addValueEventListener(valueListener)
        }

        private fun createChatRoomUser() {
            val chatRoomUserdata = chatRoomUser(
                user1 = MyApplication.auth.currentUser?.email!!,
                user2 = userData.email
            )
            chatRoomRef.child("").push().setValue(chatRoomUserdata)
        }
    }