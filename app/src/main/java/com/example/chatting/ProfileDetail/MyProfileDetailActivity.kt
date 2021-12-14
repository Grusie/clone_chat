package com.example.chatting.ProfileDetail

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.chatting.ChatRoomActivity
import com.example.chatting.Model.ChatData
import com.example.chatting.Model.ChatData.chatRoomUser
import com.example.chatting.Model.UserData
import com.example.chatting.MyApplication
import com.example.chatting.R
import com.example.chatting.databinding.ActivityMyProfileDetailBinding
import com.example.chatting.util.myCheckPermission
import java.io.File
import java.lang.Exception

class MyProfileDetailActivity : AppCompatActivity() {

    private lateinit var userData: UserData
    private lateinit var filePath: String
    private lateinit var filename: String

    val binding by lazy { ActivityMyProfileDetailBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //리사이클러 뷰 항목 클릭시 넘어온 userData 정보를 화면 뷰에 구성
        userData = intent.getParcelableExtra<UserData>("userData")!!
        editState(checkProfileUser())
        binding()

        //취소 버튼 클릭 시
        binding.myProfileBack.setOnClickListener { finish() }
        binding.myProfileBackEdit.setOnClickListener {
            editState(checkProfileUser())
        }

        //갤러리 인텐트
        val galleryIntent =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                val cursor = contentResolver.query(
                    it.data!!.data as Uri,
                    arrayOf<String>(MediaStore.Images.Media.DATA), null, null, null)

                cursor?.moveToFirst().let {
                    filePath = cursor?.getString(0) as String
                }
                if (it.resultCode == RESULT_OK) {
                    try {
                        saveStorage(filename)
                        if (filename == "profile") {
                            Glide
                                .with(this)
                                .load(it.data!!.data)
                                .apply(RequestOptions().override(150, 150))
                                .centerCrop()
                                .into(binding.myProfileImage)
                        }
                        else if(filename == "background") {
                            Glide
                                .with(this)
                                .load(it.data!!.data)
                                .centerCrop()
                                .into(binding.myProfileBackgroundImg)
                        }

                    } catch (e: Exception) {
                        Log.d("grusie", "exception : $e")
                    }

                }
            }

        //프로필 사진 클릭 시
        binding.myProfileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"

            try {
                galleryIntent.launch(intent)
            } catch (e: Exception) {
            }
            myCheckPermission(this)
            filename = "profile"
            galleryIntent.launch(intent)
        }

        //배경 사진 편집 클릭 시
        binding.myProfileEditCamera.setOnClickListener {
            val intent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            filename = "background"
            galleryIntent.launch(intent)

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

            if (newName.isEmpty()){
                Toast.makeText(this, "이름을 다시 설정해주세요.", Toast.LENGTH_SHORT).show()
            } else {
                updateAndGetValue("name", newName)
                updateAndGetValue("statusMsg", newStatusMsg)
                updateAndGetValue("profileMusic", newProfileMusic)
                editState(checkProfileUser())
            }
        }

        //채팅 버튼 클릭 시
        binding.myProfileChat.setOnClickListener {
            val documentName = MyApplication.auth.currentUser?.email

            //채팅방 검사

            //채팅방 생성
            val chatRoomId = "test"
            val data = ChatData.chatRoomUser(documentName.toString(), userData.email)
            MyApplication.realtime.child("chatRoomUser").child(chatRoomId)
                .setValue(data)

            //채팅방으로 이동
            val intent = Intent(this, ChatRoomActivity::class.java)
            intent.putExtra("userName", userData.name)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)

            finish()
        }
    }

    private fun binding() {
        binding.run {
            myProfileName.text = userData.name
            myProfileStatusMsg.text = userData.statusMsg
            myProfileMusic.text = userData.profileMusic

            val imgRefProfile =
                MyApplication.storage.reference.child("${userData.email}/profile")
            Glide.with(this@MyProfileDetailActivity)
                .load(imgRefProfile)
                .error(R.drawable.img_profile)
                .into(myProfileImage)

            val imgRefBackground =
                MyApplication.storage.reference.child("${userData.email}/background")
            Glide.with(this@MyProfileDetailActivity)
                .load(imgRefBackground)
                .into(myProfileBackgroundImg)
        }
    }

    private fun checkProfileUser(): String =
        //내 프로필인 경우 vs 내 프로필이 아닌 경우
        if(userData.email == MyApplication.auth.currentUser?.email){
            "myProfile"
        } else {
            "notMyProfile"
        }

    private fun updateAndGetValue(field: String, newValue: String) {

        //생각해보니,,, userData RV adapter가 database에 있는 정보를 가져오면 친구들 프로필 수정도 내가 하는 것이고
        //그럼 본인이 친구들 database에 접근하는 것인데,,, 이걸 방지하려면 개인이 기억하는 정보 저장 공간이 따로 필요할 것

        MyApplication.db.collection("profile")
            .document(userData.email)
            .update(field, newValue)
            .addOnSuccessListener {
                //update 성공 시 newValue get -> userData에 삽입
                MyApplication.db.collection("profile")
                    .document(userData.email)
                    .get()
                    .addOnSuccessListener { document ->
                        when(field) {
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
                        Toast.makeText(this, "설정 실패", Toast.LENGTH_SHORT).show() }
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
                }
            }
        }
    }

    private fun saveStorage(imageKind: String) {
        val storage = MyApplication.storage
        val storageRef = storage.reference
        val imgRef =
            storageRef.child("${MyApplication.auth.currentUser?.email}/${imageKind}")
        val file = Uri.fromFile(File(filePath))
        imgRef
            .putFile(file)
            .addOnSuccessListener {
            Log.d("grusie", "저장됨")
            Toast.makeText(this, "사진이 저장되었습니다.", Toast.LENGTH_SHORT).show()
        }
            .addOnFailureListener {
            Log.d("grusie", "error : $it")
        }
    }
}