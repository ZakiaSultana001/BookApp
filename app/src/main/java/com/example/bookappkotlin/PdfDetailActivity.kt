package com.example.bookappkotlin

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.bookappkotlin.databinding.ActivityPdfDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.FileOutputStream

class PdfDetailActivity : AppCompatActivity() {

    //view binding
    private lateinit var binding: ActivityPdfDetailBinding

    private companion object{
        //TAG
        const val TAG = "BOOK_DETAILS_TAG"
    }

    //book id, get from intent

    private var bookId = ""
    // get from firebase
    private var bookTitle = ""

    private var bookUrl = ""

    private var isInMyFavorite =false

    private lateinit var  firebaseAuth: FirebaseAuth

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //get book id from intent
        bookId = intent.getStringExtra("bookId")!!

        //init progress bar
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please Wait...")
        progressDialog.setCanceledOnTouchOutside(false)

        //init firebase auth

        firebaseAuth = FirebaseAuth.getInstance()
        if(firebaseAuth.currentUser!=null){
            //user is logged in,check if book is in fav or not
            checkIsFavorite()
        }


        //increment book view count, whenever this page starts
        MyApplication.incrementBookCount(bookId)


        loadBookDetails()

        //handle back button click, goback
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
        //handle click, open pdf view activity
        binding.readBookBtn.setOnClickListener {
            val intent = Intent(this, PdfViewActivity::class.java)
            intent.putExtra("bookId", bookId);
            startActivity(intent)
        }

        //handle click , download book/pdf
        binding.downloadBookBtn.setOnClickListener {
            // let's check WRITE_EXTERNAL_STORAGE permission first , if granted download book, if not granted request permission
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                Log.d(TAG, "onCreate: STORAGE PERMISSION is already granted")
                downloadBook()
            }
            else{
                Log.d(TAG, "onCreate: STORAGE PERMISSION was not granted, LETS request it")
                requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        //handle click , add/remove favorite
        binding.favouriteBtn.setOnClickListener {
            //we can add only if user is logged in
            //1)check if user is logged in or not

            if(firebaseAuth.currentUser ==null){
                //user not logged in,cant do favorite functionality
                Toast.makeText(this,"You're not logged in",Toast.LENGTH_SHORT).show()

            }
            else{
                //user is logged in,we can do favorite functionality
                if(isInMyFavorite){
                    //already in  fav,remove

                    MyApplication.removeFromFavorite(this,bookId)
                }
                else{
                    //not in fav add
                    addToFavorite()
                }
            }


        }
    }

    private val requestStoragePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){isGranted:Boolean ->
        //lets check if granted or not
        if(isGranted){
            Log.d(TAG, "onCreate: STORAGE PERMISSION is  granted")
            downloadBook()
        }
        else{
            Log.d(TAG, "onCreate: STORAGE PERMISSION is denied")
            Toast.makeText(this,"Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadBook(){
        Log.d(TAG, "downloadBook: Downloading Book")
        progressDialog.setMessage("Downloading Book")
        progressDialog.show()

        // lets download book from firebase storage using url
        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl)
        storageReference.getBytes(Constants.MAX_BYTES_PDF)
            .addOnSuccessListener {bytes ->
                Log.d(TAG, "downloadBook: Book downloaded...")
                saveToDownloadsFolder(bytes)
            }
            .addOnFailureListener {e->
                progressDialog.dismiss()
                Log.d(TAG, "downloadBook: Failed to download book due to ${e.message}")
                Toast.makeText(this,"Failed to download book due to ${e.message}", Toast.LENGTH_SHORT).show()
            }

    }

    private fun saveToDownloadsFolder(bytes: ByteArray?) {

        Log.d(TAG, "saveToDownloadsFolder: saving downloaded book")
        val nameWithExtention = "${System.currentTimeMillis()}.pdf"

        try {
            val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsFolder.mkdirs()    //create folder if not exist

            val filePath = downloadsFolder.path +"/"+nameWithExtention

            val out = FileOutputStream(filePath)
            out.write(bytes)
            out.close()

            Toast.makeText(this,"Saved to Downloads Folder", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "saveToDownloadsFolder: Saved to Downloads Folder")
            progressDialog.dismiss()
            incrementDownloadCount()
        }
        catch (e: Exception){
            progressDialog.dismiss()
            Log.d(TAG, "saveToDownloadsFolder: Failed to save due to ${e.message}")
            Toast.makeText(this,"Failed to save due to ${e.message}", Toast.LENGTH_SHORT).show()
        }

    }

    private fun incrementDownloadCount(){
        // increment downloads count to firebase db

        Log.d(TAG, "incrementDownloadCount: ")

        // step: 1  Get previous downloads count
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // get downloads count
                    var downloadsCount = "${snapshot.child("downloadsCount").value}"

                    Log.d(TAG, "onDataChange: Current Downloads Count: $downloadsCount")
                    
                    if (downloadsCount == "" || downloadsCount == "null"){
                        downloadsCount = "0"
                    }
                    
                    // convert to long and increment 1
                    val newDownloadCount: Long = downloadsCount.toLong() + 1
                    Log.d(TAG, "onDataChange: New Downloads Count: $downloadsCount")
                    
                    // setup data to update to db
                    val hashMap: HashMap<String, Any> = HashMap()
                    hashMap["downloadsCount"] = newDownloadCount

                    // step: 2  Update new incremented downloads count to db
                    val dbRef = FirebaseDatabase.getInstance().getReference("Books")
                    dbRef.child(bookId)
                        .updateChildren(hashMap)
                        .addOnSuccessListener {
                            Log.d(TAG, "onDataChange: Downloads count incremented")
                        }
                        .addOnFailureListener {e->
                            Log.d(TAG, "onDataChange: FAILED to increment due to ${e.message}")
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    
                }
            })

    }

    private fun loadBookDetails() {
        // Books > BookId > Details
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // get data
                    val categoryId = "${snapshot.child("categoryId").value}"
                    val description = "${snapshot.child("description").value}"
                    val downloadsCount = "${snapshot.child("downloadsCount").value}"
                    val timestamp = "${snapshot.child("timestamp").value}"
                    bookTitle = "${snapshot.child("title").value}"
                    val uid = "${snapshot.child("uid").value}"
                    bookUrl = "${snapshot.child("url").value}"
                    val viewsCount = "${snapshot.child("viewsCount").value}"

                    //format date
                    val date = MyApplication.formatTimeStamp(timestamp.toLong())

                    //load pdf category
                    MyApplication.loadCategory(categoryId, binding.categoryTv)

                    //load pdf thumbnail, pages count,
                    MyApplication.loadPdfFormUrlSinglePage("$bookUrl", "$bookTitle", binding.pdfView, binding.progressBar, binding.pagesTv)

                    //load pdf size
                    MyApplication.loadPdfSize("$bookUrl", "$bookTitle", binding.sizeTv)


                    //set data
                    binding.titleTv.text = bookTitle
                    binding.descriptionTv.text = description
                    binding.viewsTv.text = viewsCount
                    binding.downloadsTv.text = downloadsCount
                    binding.dateTv.text = date

                }
                override fun onCancelled(error: DatabaseError) {

                }

            })
    }


    private  fun  checkIsFavorite(){

        Log.d(TAG,"checkIsFavorite:checking if book is in fav or not ")


        val ref =FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    isInMyFavorite =snapshot.exists()
                    if (isInMyFavorite){
                        //available in favorite
                        Log.d(TAG,"onDataChange:available in favorite")

                        //set drawable top icon
                        binding.favouriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0,R.drawable.ic_favorite_filled_white,0,0)
                        binding.favouriteBtn.text ="Remove Favorite"
                    }
                    else{
                        //not available in favorite

                        Log.d(TAG,"onDataChange:not available in favorite")

                        //set drawable top icon
                        //set drawable top icon
                        binding.favouriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0,R.drawable.ic_favorite_border_white,0,0)
                        binding.favouriteBtn.text ="Add Favorite"

                    }

                }

                override fun onCancelled(error: DatabaseError) {

                }

            })


    }

    private fun addToFavorite(){
        Log.d(TAG,"addToFavorite:Adding to fav")
        val timestamp =System.currentTimeMillis()

        //setup data to add in db
        val hashMap =HashMap<String,Any>()
        hashMap["bookId"]=bookId
        hashMap["timestamp"]=timestamp

        //save to db
        val ref =FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child((bookId))
            .setValue(hashMap)
            .addOnSuccessListener {
                //added to favorite
                Log.d(TAG,"addToFavorite:Adding to fav")
                Toast.makeText(this,"Added to favorite ",Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e->
                //failed to add o fav
                Log.d(TAG,"addToFavorite:Failed to add to fav due to ${e.message}")
                Toast.makeText(this,"Failed to add to fav due to ${e.message}",Toast.LENGTH_SHORT).show()
            }

    }



}