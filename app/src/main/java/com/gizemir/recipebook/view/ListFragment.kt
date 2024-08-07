package com.gizemir.recipebook.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.gizemir.recipebook.adapter.RecipeAdapter
import com.gizemir.recipebook.databinding.FragmentListBinding
import com.gizemir.recipebook.model.Recipe
import com.gizemir.recipebook.roomdb.RecipeDAO
import com.gizemir.recipebook.roomdb.RecipeDatabase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers


class ListFragment : Fragment() {
    private var _binding: FragmentListBinding? = null

    private val binding get() = _binding!!

    private lateinit var db : RecipeDatabase
    private  lateinit var recipeDAO: RecipeDAO
//yaptığımız işlemleri kaydetmek için
    private val mDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = Room.databaseBuilder(requireContext(), RecipeDatabase::class.java, "Recipes").build()
        recipeDAO = db.recipeDao()

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.floatingActionButton.setOnClickListener { newAdd(it) }
        binding.recipeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        getData()
    }
    private  fun getData(){
        //RecipeFragmentinde save ile kaydettiğimiz verileri bu fonksiyon ile çekiyoruz ve sistemde gözüküyor
        mDisposable.add(
            recipeDAO.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                //verileri alma işleminden sonra bu fonksiyonu uygula
                .subscribe(this:: handleResponse)
        )
    }
    private  fun handleResponse(recipes: List<Recipe>){
        //kaydedilmiş tüm verileri Listfragmentinde göster
        val adapter = RecipeAdapter(recipes)
        binding.recipeRecyclerView.adapter = adapter
    }
    fun newAdd(view: View){
        //butona tıklandığında diğer fragmente geçecek
        val action = ListFragmentDirections.actionListFragmentToRecipeFragment("new", 0)
        Navigation.findNavController(view).navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        mDisposable.clear()
    }

}