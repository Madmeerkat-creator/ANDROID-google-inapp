package com.munice.miraclenighttest.Login

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.munice.miraclenighttest.MainActivity
import com.munice.miraclenighttest.R
import com.munice.miraclenighttest.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    var TAG: String = "mainTag"
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var binding: ActivityLoginBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()


        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Log.d("mainTag", "registerForActivityResult")
            Log.d("mainTag", it.resultCode.toString())
            if (it.resultCode == RESULT_OK) {
                val result = Auth.GoogleSignInApi.getSignInResultFromIntent(it.data!!)
                if(result!!.isSuccess){
                    val account = result.signInAccount
                    firebaseAuthWithGoogle(account)
                }
            }
        }

        binding.googleSignInBtn.setOnClickListener{googleLogin()}

    }

    private fun googleLogin() {
        Log.d("mainTag", "googleLogin start")
        val signInIntent = googleSignInClient!!.signInIntent
        activityResultLauncher.launch(signInIntent)
        Log.d("mainTag", "googleLogin finish")
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        Log.d("mainTag", "firebaseAuthWithGoogle")
        var credentail = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth?.signInWithCredential(credentail)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    createUserInfo(task)
                    moveMainPage(task.result?.user)
                } else {
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun moveMainPage(user: FirebaseUser?) {
        Log.d(TAG, "moveMainPage")
        Log.d("mainTag", user?.uid.toString())
        if (user != null) {
            startActivity(
                Intent(this, MainActivity::class.java)
            )
        }
    }

    private fun createUserInfo(task: Task<AuthResult?>){
        Log.d(TAG, "createUserInfo")
        if(task.result?.additionalUserInfo!!.isNewUser) {
            Log.d(TAG, "start")
            val userEmail = task.result?.user!!.email.toString()
            val currentTime = System.currentTimeMillis()
            val userInfo = UserInfo(
                userName = userEmail,
                coin = 0,
                createdDate = currentTime,
                modifiedDate = currentTime
            )
            firestore.collection("User")
                .document(auth.currentUser!!.uid)
                .set(userInfo)
                .addOnSuccessListener {
                    Toast.makeText(this, "데이터가 추가되었습니다", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener{
                    Log.d(TAG, it.toString())
                }
            setResult(Activity.RESULT_OK)
            finish()
        }else{
            return
        }
    }

    override fun onStart(){
        super.onStart()
        Log.d("mainTag", "onStart")
        moveMainPage(auth?.currentUser)
    }
}