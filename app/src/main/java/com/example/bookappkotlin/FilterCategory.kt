package com.example.bookappkotlin

import android.widget.Filter


class FilterCategory : Filter {

    //arraylist in which we want to search

    private var filterList:ArrayList<ModelCategory>
    //adapter in which filter need to be implemented

    private var  adapterCategory:AdapterCategory

    //constructor
    constructor(filterList: ArrayList<ModelCategory>, adapterCategory: AdapterCategory) : super() {
        this.filterList = filterList
        this.adapterCategory = adapterCategory
    }

    override fun performFiltering(constraint: CharSequence?): FilterResults {

        var constraint = constraint
        val results = FilterResults()


        //value should not be null and not empty

        if (constraint != null && constraint.isNotEmpty()) {
            //searched value is nor null not empty



            //change to upper case, or lower case to avoid case sensitivity

            constraint = constraint.toString().uppercase()
            val filteredModel: ArrayList<ModelCategory> = ArrayList()
            for (i in 0 until filterList.size) {
                //validate
                if (filterList[i].category.uppercase().contains(constraint)) {
                    //add  to filtered list

                    filteredModel.add(filterList[i])

                }
            }

                results.count = filteredModel.size
                results.values = filteredModel
            }
            else{
                //search value is either null or empty
                results.count=filterList.size
            results.values =filterList

            }

            return results// dont miss it


    }

    override fun publishResults(constarint: CharSequence?, results: FilterResults) {
        //apply filter changes

         adapterCategory.categoryArrayList = results.values as ArrayList<ModelCategory>

        //notify changes

        adapterCategory.notifyDataSetChanged()
    }
}