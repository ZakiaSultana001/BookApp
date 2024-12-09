package com.example.bookappkotlin

import android.widget.Filter

class FilterPdfUser: Filter {

    //arraylist in which we want to search
    var filterList: ArrayList<ModelPdf>
    //adapter in which filter need to be implemented
    var adapterPdfUser: AdapterPdfUser

    //constructor
    constructor(filterList: ArrayList<ModelPdf>, adapterPdfUser: AdapterPdfUser) : super() {
        this.filterList = filterList
        this.adapterPdfUser = adapterPdfUser
    }

    override fun performFiltering(constraint: CharSequence): FilterResults {

        var constraint: CharSequence? = constraint

        val results = FilterResults()
        //value to be searched should not be null and not empty
        if (constraint != null && constraint.isNotEmpty()){
            //not null nor empty

            //change to upper case, or lower case to remove case sensitivity
            constraint = constraint.toString().uppercase()
            val filteredModels = ArrayList<ModelPdf>()
            for (i in filterList.indices){
                //validate if match
                if (filterList[i].title.uppercase().contains(constraint)){
                    //searched value matched with title , add to list
                    filteredModels.add(filterList[i])
                }

            }
            //return filtered list & size
            results.count = filteredModels.size
            results.values = filteredModels

        }
        else{
            // either it is null or is empty

            //return original list and size
            results.count = filterList.size
            results.values = filterList
        }

        return results
    }

    override fun publishResults(constraint: CharSequence, results: FilterResults) {
        // apply filter changes
        adapterPdfUser.pdfArrayList =results.values as ArrayList<ModelPdf> /* = java.util.ArrayList<com.example.bookappkotlin.ModelPdf> */

        //notify changes
        adapterPdfUser.notifyDataSetChanged()



    }


}