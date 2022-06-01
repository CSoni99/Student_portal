package com.chaitanya.student_portal

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.google.android.gms.safetynet.SafetyNet
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private var forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null
    private var mCallBacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks? = null
    private var mVerificationId: String? = ""
    private lateinit var firebaseAuth: FirebaseAuth

    private val TAG = "MAIN_TAG"

    private lateinit var progressDialog : ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN ,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait...")
        progressDialog.setCanceledOnTouchOutside(false)

        mCallBacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                SafetyNet.getClient(this@MainActivity)
                    .verifyWithRecaptcha("AIzaSyCWuuVk0NOdy2dkhepskGIxxMcuoUZV3Nw")
                val currentUser = firebaseAuth.currentUser
                if(currentUser != null){
                    val database = FirebaseDatabase.getInstance().getReference("Users")
                    database.child(phoneEt.text.toString()).get().addOnSuccessListener {
                        if(it.child("emailId").value.toString() != "null"){
                            val intent = Intent(this@MainActivity,Dashboard::class.java)
                            startActivity(intent)
                            this@MainActivity.finish()
                        }

                        else{
                            val i = Intent(this@MainActivity,CreateAccount::class.java)
//                            val i = Intent(this@MainActivity,MainActivity::class.java)
                            startActivity(i)
                            this@MainActivity.finish()
                        }
                    }
                }

            }
            override fun onVerificationFailed(e: FirebaseException) {
                progressDialog.dismiss()
                Toast.makeText(this@MainActivity , "${e.message}" , Toast.LENGTH_LONG).show()
            }

            override fun onCodeSent(verificationId: String , token: PhoneAuthProvider.ForceResendingToken) {
                super.onCodeSent(verificationId , token)
                Log.d(TAG, "onCodeSent : $verificationId")
                mVerificationId = verificationId
                forceResendingToken = token
                progressDialog.dismiss()
                //hide phone Layout show code layout

                Toast.makeText(this@MainActivity , "Verification Code Sent" , Toast.LENGTH_SHORT).show()

                phoneEt.isEnabled = false

                sendOTP_btn.visibility= View.GONE
                validate_otp_btn.visibility = View.VISIBLE
                codeEt.visibility = View.VISIBLE
                ResendOtp.visibility= View.VISIBLE
            }

        }
//          to get OTP
        sendOTP_btn.setOnClickListener {
            val phone = "+91"+ phoneEt.text.toString().trim()

            //Validate mobile no.
            if(TextUtils.isEmpty(phone)){
                Toast.makeText(this , "Please Enter Phone Number" , Toast.LENGTH_SHORT).show()
            }else{
                startPhoneNumberVerification(phone)
            }
        }
//        Submit otp for verification
        validate_otp_btn.setOnClickListener{
            val code = codeEt.text.toString().trim()

            //Validate mobile no.
            if(TextUtils.isEmpty(code)){
                Toast.makeText(this , "Please Enter verification code}" , Toast.LENGTH_SHORT).show()
            }else{
                verifyPhoneNumberWithCode(mVerificationId, code)
            }
        }
//        Resend OTP
        ResendOtp.setOnClickListener{
            val phone = "+91"+phoneEt.text.toString().trim()

            //Validate mobile no.
            if(TextUtils.isEmpty(phone)){
                Toast.makeText(this , "Please Enter Phone Number" , Toast.LENGTH_SHORT).show()
            }else{
                resendVerificationCode(phone,forceResendingToken)
            }
        }

    }
    override fun onStart() {
        super.onStart()
        val currentUser = firebaseAuth.currentUser
        if(currentUser != null){
            val database = FirebaseDatabase.getInstance().getReference("Users")
            database.child(phoneEt.text.toString()).get().addOnSuccessListener {
                if(it.child("name").value.toString() != "null"){
                    val intent = Intent(this,Dashboard::class.java)
                    startActivity(intent)
                    this@MainActivity.finish()
                }
                else{
                    val phone = "+91"+phoneEt.text.toString().trim()
                    val i = Intent(this,CreateAccount::class.java)
//                    val i = Intent(this,MainActivity::class.java)
//                    i.putExtra(phoneEt.text.toString(),phone)
                    i.putExtra("phone",phone)
                    startActivity(i)
                    this@MainActivity.finish()
                }
            }
        }
        else{
            phoneEt.visibility = View.VISIBLE
            sendOTP_btn.visibility= View.VISIBLE
            validate_otp_btn.visibility = View.GONE
            codeEt.visibility = View.GONE
            ResendOtp.visibility= View.GONE
        }
    }

    private fun startPhoneNumberVerification(phone:String){
        progressDialog.setMessage("Verifying phone Number...")
        progressDialog.show()

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber( phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this@MainActivity)
            .setCallbacks(mCallBacks!!) //it --> mCallbacks
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)


    }

    private fun resendVerificationCode(phone : String, token : PhoneAuthProvider.ForceResendingToken?){
        progressDialog.setMessage("Resending Verification Code...")
        progressDialog.show()

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber( phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(mCallBacks!!) //it --> mCallbacks
            .setForceResendingToken(token!!)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyPhoneNumberWithCode(verificationId:String?, code:String){
        progressDialog.setMessage("Verifying Security code...")
        progressDialog.show()

        val credential = PhoneAuthProvider.getCredential(verificationId.toString() , code)
        signinWithPhoneAuthCredential(credential)
    }

    private fun signinWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        progressDialog.setMessage("Logging In...")
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener { //Tasks for login sucess

                val phone = firebaseAuth.currentUser?.phoneNumber.toString()
                //Start profile Activity
                val database = FirebaseDatabase.getInstance().getReference("Users")
                database.child(phone).get().addOnSuccessListener {
                    if(it.child("name").value.toString() == "null"){
                        val i = Intent(this@MainActivity,CreateAccount::class.java)
//                        val i = Intent(this@MainActivity,MainActivity::class.java)
                        startActivity(i)
                        Toast.makeText(this , "Logged In as $phone" , Toast.LENGTH_SHORT).show()
                        progressDialog.dismiss()
                        this@MainActivity.finish()
                    }
                    else{
                        val i = Intent(this@MainActivity,Dashboard::class.java)
                        startActivity(i)
                        Toast.makeText(this , "Logged In as $phone" , Toast.LENGTH_SHORT).show()
                        progressDialog.dismiss()
                        this@MainActivity.finish()
                    }
                }
            }
            .addOnFailureListener { e->
//                tasks for login failed
                progressDialog.dismiss()
                Toast.makeText(this , "${e.message}" , Toast.LENGTH_LONG).show()
            }

    }
}