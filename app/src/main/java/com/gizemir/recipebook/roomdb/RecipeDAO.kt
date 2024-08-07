package com.gizemir.recipebook.roomdb

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.gizemir.recipebook.model.Recipe
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable

@Dao
interface RecipeDAO {
    //oluşturduğumuz Recipe modeli(tablosuyla) queryler yazdık
    @Query("SELECT *FROM Recipe")
    fun getAll() : Flowable<List<Recipe>>
    @Query("SELECT * FROM Recipe WHERE id = :id")
    fun findById(id: Int) : Flowable<Recipe>
    //Insert into.. gibi komutlar yazmadan tek şeyle halletik
    @Insert
    fun insert(recipe: Recipe): Completable
    @Delete
    fun delete(recipe: Recipe): Completable
}