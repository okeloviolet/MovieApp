package com.odhiambopaul.movie.ui.home

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.odhiambopaul.movie.App
import com.odhiambopaul.movie.R
import com.odhiambopaul.movie.databinding.ActivityHomeBinding
import com.odhiambopaul.movie.di.component.DaggerApplicationComponent
import com.odhiambopaul.movie.ui.search.SearchActivity
import com.odhiambopaul.movie.util.apiKey
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_home.*
import javax.inject.Inject
import kotlin.math.round

class HomeActivity : AppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var binding: ActivityHomeBinding
    private var errorSnackbar: Snackbar? = null
    private lateinit var subscription: Disposable
    private val errorMessage: MutableLiveData<Int> = MutableLiveData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DaggerApplicationComponent.factory().create(this.application as App).inject(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        val viewModel = ViewModelProvider(this, viewModelFactory).get(HomeViewModel::class.java)
        binding.viewModel = viewModel
        //getting the screen size
        val display: Display = this.windowManager.defaultDisplay
        val outmetrics = DisplayMetrics()
        display.getMetrics(outmetrics)
        val density: Float = resources.displayMetrics.density
        val dpwidth: Float = outmetrics.widthPixels / density
        val columns: Int = round(dpwidth / 190).toInt()

        subscription = viewModel.getMovies()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { onRetrieveMovieListStart() }
            .doOnTerminate { onRetrieveMovieListFinish() }
            .subscribe({ result ->
                viewModel.getNowPlaying()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ data ->
                        viewModel.getTopRated()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ response ->
                                binding.movieRecyclerView.apply {
                                    setHasFixedSize(true)
                                    layoutManager = GridLayoutManager(this@HomeActivity, columns)
                                    adapter =
                                        HomeAdapter(result.results + data.results + response.results, context)
                                }
                            }, { t ->
                                kotlin.run {
                                    Log.d("Error:::", t?.localizedMessage!!)
                                    errorMessage.value = R.string.movie_error
                                    showError(errorMessage.value!!)
                                }
                            })

                    }, { t ->
                        kotlin.run {
                            Log.d("Error:::", t?.localizedMessage!!)
                            errorMessage.value = R.string.movie_error
                            showError(errorMessage.value!!)
                        }
                    })
            }, { t ->
                kotlin.run {
                    Log.d("Error:::", t?.localizedMessage!!)
                    errorMessage.value = R.string.movie_error
                    showError(errorMessage.value!!)
                }
            })
        viewModel.searchMovie(apiKey, "need")
    }

    private fun showError(@StringRes errorMessage: Int) {
        errorSnackbar = Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_INDEFINITE)
        errorSnackbar?.setAction(R.string.retry) {
            hideError()
        }
        errorSnackbar?.show()
    }

    private fun hideError() {
        errorSnackbar?.dismiss()
    }

    private fun onRetrieveMovieListStart() {
        progressBar.visibility = View.VISIBLE
        errorMessage.value = null
    }

    private fun onRetrieveMovieListFinish() {
        progressBar.visibility = View.GONE
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId)
        {
            R.id.search_menu->{
                startActivity(Intent(this,SearchActivity::class.java))
                true
            }
            R.id.about_menu->{
                Toast.makeText(this, "Will be implemented soon",Toast.LENGTH_LONG).show()
                return true
            }
            else->super.onOptionsItemSelected(item)
        }
    }
}