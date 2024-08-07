package com.gizemir.recipebook.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Dimension
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.room.Room
import com.gizemir.recipebook.databinding.FragmentRecipeBinding
import com.gizemir.recipebook.model.Recipe
import com.gizemir.recipebook.roomdb.RecipeDAO
import com.gizemir.recipebook.roomdb.RecipeDatabase
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.ByteArrayOutputStream


class RecipeFragment : Fragment() {
    private var _binding: FragmentRecipeBinding? = null
    private val binding get() = _binding!!
    //izin verilmesi için
    private lateinit var permissionLauncher : ActivityResultLauncher<String>
    //galeriye erişebilmek için
    private lateinit var activityResultLauncher:  ActivityResultLauncher<Intent>
    private  var selectedImage: Uri?= null
    private  var selectedBitmap: Bitmap? = null
    private  val mDisposable = CompositeDisposable()
    private  var selectedRecipe: Recipe? = null

    private lateinit var db : RecipeDatabase
    private  lateinit var recipeDAO: RecipeDAO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerLauncher()
        db = Room.databaseBuilder(requireContext(), RecipeDatabase::class.java, "Recipes").build()
        recipeDAO = db.recipeDao()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRecipeBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imageView.setOnClickListener { selectImage(it) }
        binding.deleteButton.setOnClickListener { delete(it) }
        binding.saveButton.setOnClickListener { save(it) }

        arguments?.let {
            val information = RecipeFragmentArgs.fromBundle(it).information
            if(information == "new"){
                //add new recipe
                selectedRecipe = null
                binding.deleteButton.isEnabled = false
                binding.saveButton.isEnabled = true
                binding.nameText.setText("")
                binding.ingredientsText.setText("")
            }
            else{
                //no add recipe(show old recipe)
                //eski eklenmiş tarif gösteriliyor
                binding.deleteButton.isEnabled = true
                binding.saveButton.isEnabled = false

                val id = RecipeFragmentArgs.fromBundle(it).id
                mDisposable.add(
                    recipeDAO.findById(id)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        //bu subscribe'ın içindeki iiçin yeni işlevli bir fonksiyon oluşturacağız
                        .subscribe(this::handleResponse)
                )
            }
        }
    }
    private  fun handleResponse(recipe: Recipe){
        val bitmap = BitmapFactory.decodeByteArray(recipe.image, 0, recipe.image.size)
        binding.imageView.setImageBitmap(bitmap)
        binding.nameText.setText(recipe.name)
        binding.ingredientsText.setText((recipe.ingredients))
        selectedRecipe = recipe
    }
    fun registerLauncher(){
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->
            //kullanıcı galeriye gittiğinde eğer görseli seçerse Result Okay döner, Seçmezse ResultCanceled
            if(result.resultCode == AppCompatActivity.RESULT_OK){
                //kullanıcı görsel seçmiştir ve artık bu datayı kullanabilirz
                val intentFromResult = result.data
                if(intentFromResult != null){
                    //kullanıcının seçtiği görselin nerede kayıtlı olduğunu gösterir
                    //uri
                    selectedImage= intentFromResult.data
                    try {
                        if(Build.VERSION.SDK_INT >= 28){
                            //yeni yöntem

                            val source = ImageDecoder.createSource(requireActivity().contentResolver, selectedImage!!)
                            selectedBitmap = ImageDecoder.decodeBitmap(source)
                            binding.imageView.setImageBitmap(selectedBitmap)
                        }else{
                            //eski yöntem
                            selectedBitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, selectedImage)
                            binding.imageView.setImageBitmap(selectedBitmap)
                        }
                    }catch (e: Exception){
                        println(e.localizedMessage)
                    }
                }
            }
        }
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result->
              if(result){
                  //izin verildi
                  //galeriye gidebiliriz
                  val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                  activityResultLauncher.launch(intentToGallery)
              }else{
                  //izin verilmedi
                  Toast.makeText(requireContext(), "Not allowed!", Toast.LENGTH_LONG).show()
              }
        }
    }
    fun selectImage(view: View){

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            // Manifeste yazılan iznin daha önce alınıp alınmadığını kontrol eder
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED){
                //eğer izin verilmediyse izin istememiz gerekiyor
                if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.READ_MEDIA_IMAGES)){
                    //snackbar göstermemiz lazım, kullanıcıdan neden izin istediğimizi bir kez daha açıklayan izin
                    Snackbar.make(view, "to select an image we have to access the gallery", Snackbar.LENGTH_INDEFINITE).setAction(
                        "Allow it",
                        View.OnClickListener {
                            //izin isteyeceğiz
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        }
                    ).show()
                }else{
                    //izin isteyeceğiz
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }else{
                //ilk if kontrolünde izin istenmiş mi diye kontol ettik. İf kontrolünü geçemeyip buraya geldiyse izin istenmiş demektir direkt galeriye gideceğiz
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }

        //sdk 33'ten küçükse yani eskiyse
        }else{
            // Manifeste yazılan iznin daha önce alınıp alınmadığını kontrol eder
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                //eğer izin verilmediyse izin istememiz gerekiyor
                if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)){
                    //snackbar göstermemiz lazım, kullanıcıdan neden izin istediğimizi bir kez daha açıklayan izin
                    Snackbar.make(view, "to select an image we have to access the gallery", Snackbar.LENGTH_INDEFINITE).setAction(
                        "Allow it",
                        View.OnClickListener {
                            //izin isteyeceğiz
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }).show()
                }else{
                    //izin isteyeceğiz
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }else{
                //ilk if kontrolünde izin istenmiş mi diye kontol ettik. İf kontrolünü geçemeyip buraya geldiyse izin istenmiş demektir direkt galeriye gideceğiz
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        }
    }
    fun delete(view: View){
        if(selectedRecipe != null){
            mDisposable.add(
                recipeDAO.delete(recipe = selectedRecipe!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    //tıklanınca geri gideceğiz
                    .subscribe(this::handleResponseForInsert)
            )
        }
    }
    fun save(view: View){
        val name = binding.nameText.text.toString()
        val ingredient = binding.ingredientsText.text.toString()
        //uygulamaya yüklenen resmi createSmallBitmap fonksiyonuyla bitmape çevirmiştik. bunu da byte dizisine çevirmemiz lazım
        if(selectedBitmap != null){
            val smallBitmap = createSmallBitmap(selectedBitmap!!, 300)
            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG, 50, outputStream)
            val byteArray = outputStream.toByteArray()
            //yemeğin ismini, içeriğini ve görselini aldık ancak henüz kaydetmedik
            val recipe = Recipe(name, ingredient, byteArray)
            //Threading
            //RxJava
            //add işlemi yapılacak
            mDisposable.add(//işlemi yapar
                recipeDAO.insert(recipe)
                    //işlemi arka planda yapar
                    //hem internette hem veritabanı işlemlerinde io kullanılır
                    .subscribeOn(Schedulers.io())
                    //hem işlemi ön planda gösterip kitlemeyecek hiçbir şeyi
                    //sonucu hangi thread'te göstericez, veriyi arka plandan aldık, burada mainThread'te göstericez
                    .observeOn(AndroidSchedulers.mainThread())
                    //yukarıdaki kodlar sonucunda ne olacağını bir fonksiyona atıyoruz
                    //hem de işlem bittiğinde handleREs.. fonksiyonunu çalıstıracak
                    .subscribe(this::handleResponseForInsert))

        }
    }
private  fun handleResponseForInsert(){
    //main threadte(recipeFragment'te işler bittikten sonra bir önceki fragmente dön
    val action = RecipeFragmentDirections.actionRecipeFragmentToListFragment()
    Navigation.findNavController(requireView()).navigate(action)
}

    private  fun createSmallBitmap(userSelectedBitmap:Bitmap, maxDimension: Int) : Bitmap{
        var height = userSelectedBitmap.height
        var width = userSelectedBitmap.width

        val bitMapRatio: Double = width.toDouble() / height.toDouble()
        if(bitMapRatio > 1){
            //görsel yatay
            width = maxDimension
            val shortHeight = width/bitMapRatio
            height = shortHeight.toInt()

        }else{
            //görsel dikey
            height = maxDimension
            val shortWidth = height*bitMapRatio
            width = shortWidth.toInt()

        }
        return Bitmap.createScaledBitmap(userSelectedBitmap, width, height, true)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        mDisposable.clear()
    }

}