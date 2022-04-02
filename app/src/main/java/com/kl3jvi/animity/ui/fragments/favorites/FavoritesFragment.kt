package com.kl3jvi.animity.ui.fragments.favorites

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.kl3jvi.animity.R
import com.kl3jvi.animity.data.model.ui_models.AnimeMetaModel
import com.kl3jvi.animity.databinding.FragmentFavoritesBinding
import com.kl3jvi.animity.favoriteAnime
import com.kl3jvi.animity.ui.activities.main.MainActivity
import com.kl3jvi.animity.ui.base.viewBinding
import com.kl3jvi.animity.utils.collectFlow
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class FavoritesFragment : Fragment(R.layout.fragment_favorites) {

    private val viewModel: FavoritesViewModel by viewModels()
    private val binding: FragmentFavoritesBinding by viewBinding()
    private var shouldRefreshFavorites: Boolean = false
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        initViews()
    }

    private fun observeViewModel() {
        observeAniList()
    }

    private fun initViews() {

        viewModel.shouldRefresh.value = shouldRefreshFavorites
        binding.swipeLayout.setOnRefreshListener {
            viewModel.shouldRefresh.value = shouldRefreshFavorites
            shouldRefreshFavorites = !shouldRefreshFavorites
            observeAniList()

        }
    }


    private fun observeAniList() {
        collectFlow(viewModel.favoriteAniListAnimeList) { animeList ->
            val list = animeList?.data?.user?.favourites?.anime?.edges?.map {
                AnimeMetaModel(
                    id = it?.node?.id ?: 0,
                    title = it?.node?.title?.romaji.toString(),
                    imageUrl = it?.node?.coverImage?.large.toString(),
                    categoryUrl = null
                )
            }


            binding.favoritesRecycler.layoutManager = GridLayoutManager(requireContext(), 3)
            binding.favoritesRecycler.withModels {
                if (!list.isNullOrEmpty()) {
                    list.forEach { animeMetaModel ->
                        favoriteAnime {
                            id(animeMetaModel.id)
                            clickListener { _ ->
//                                val directions =
//                                    FavoritesFragmentDirections.actionNavigationFavoritesToNavigationDetails(
//                                        animeMetaModel
//                                    )
//                                findNavController().navigate(directions)
                            }
                            animeInfo(animeMetaModel)
                        }
                    }
                }
                binding.favoritesRecycler.isVisible = !list.isNullOrEmpty()
                binding.nothingSaved.isVisible = list.isNullOrEmpty()
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.swipeLayout.isRefreshing = isLoading
    }


    override fun onResume() {
        super.onResume()
        if (requireActivity() is MainActivity) {
            (activity as MainActivity?)?.showBottomNavBar()
        }
    }
}
