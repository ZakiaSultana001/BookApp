package com.example.bookappkotlin

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.example.bookappkotlin.databinding.RowPdfUserBinding

class AdapterPdfUser : RecyclerView.Adapter<AdapterPdfUser.HolderPDfUser>, Filterable{

    //context get using constructor
    private var context: Context

    //arrayList to hold pdfs,get using constructors
   public var pdfArrayList: ArrayList<ModelPdf>          //to access in filter class , make public

   //arraylist to hold filtered pdfs
   public var filterList: ArrayList<ModelPdf>


    //viewBinding row_pdf_user.xml => RowPdfUserBinding
    private lateinit var binding: RowPdfUserBinding

    private var  filter: FilterPdfUser? =null

    //now we will create  a filter class to enable searching

    constructor(context: Context, pdfArrayList: ArrayList<ModelPdf>) {
        this.context = context
        this.pdfArrayList = pdfArrayList
        this.filterList =pdfArrayList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderPDfUser {
        // inflate/bind layout row_pdf_user.xml
        binding = RowPdfUserBinding.inflate(LayoutInflater.from(context), parent, false)

        return HolderPDfUser(binding.root)
    }

    override fun onBindViewHolder(holder: HolderPDfUser, position: Int) {
        /*Get data,set data, handle click etc */

        // get data
        val model = pdfArrayList[position]
        val bookId = model.id
        val categoryId = model.categoryId
        val title = model.title
        val description = model.description
        val uid = model.uid
        val url = model.url
        val timestamp = model.timestamp

        //convert time
        val date = MyApplication.formatTimeStamp(timestamp)

        //set data
        holder.titleTv.text = title
        holder.descriptionTv.text = description
        holder.dateTv.text = date

        MyApplication.loadPdfFormUrlSinglePage(url, title, holder.pdfView, holder.progressBar, null)   //no need number of pages so pass bull

        MyApplication.loadCategory(categoryId, holder.categoryTv)

        MyApplication.loadPdfSize(url, title, holder.sizeTv)

        //handle click, open pdf details page
        holder.itemView.setOnClickListener {
            //pass book id in intent , that will be used to get pdf info
            val intent = Intent(context, PdfDetailActivity::class.java)
            intent.putExtra("bookId",bookId)
            context.startActivity(intent)

        }
    }

    override fun getItemCount(): Int {
        return pdfArrayList.size       //return array list size/ number of records
    }


    override fun getFilter(): Filter {

        if(filter ==null){
            filter =FilterPdfUser(filterList,this)
        }
        return filter as FilterPdfUser

    }
    /*ViewHolder class row_pdf_user.xml */
    inner class HolderPDfUser(itemView: View): RecyclerView.ViewHolder(itemView){
        //init UI components of row_pdf_user.xml
        var pdfView = binding.pdfView
        var progressBar = binding.progressBar
        var titleTv = binding.titleTv
        var descriptionTv = binding.descriptionTv
        var categoryTv = binding.categoryTv
        var sizeTv = binding.sizeTv
        var dateTv = binding.dateTv
    }



}