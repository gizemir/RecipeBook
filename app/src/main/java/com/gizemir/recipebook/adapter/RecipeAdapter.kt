package com.gizemir.recipebook.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.gizemir.recipebook.databinding.RecyclerRowBinding
import com.gizemir.recipebook.model.Recipe
import com.gizemir.recipebook.view.ListFragmentDirections
import com.gizemir.recipebook.view.RecipeFragmentDirections

class RecipeAdapter(val recipeList: List<Recipe>): RecyclerView.Adapter<RecipeAdapter.RecipeHolder>(){
    class  RecipeHolder(val binding: RecyclerRowBinding): RecyclerView.ViewHolder(binding.root){

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeHolder {
        val recyclerRowBinding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecipeHolder(recyclerRowBinding)
    }

    override fun getItemCount(): Int {
        return recipeList.size
    }

    override fun onBindViewHolder(holder: RecipeHolder, position: Int) {
        //tıklandığında ne olacak
        holder.binding.recyclerViewTextView.text = recipeList[position].name
        //oluşan listedekilerin isimlerini ekranda göster
        holder.itemView.setOnClickListener{
            //ekranda oluşan listedeki herhangi bir elemana tıklandığında
            val action = ListFragmentDirections.actionListFragmentToRecipeFragment(information = "old", id = recipeList[position].id)
            Navigation.findNavController(it).navigate(action)
        }

    }

}