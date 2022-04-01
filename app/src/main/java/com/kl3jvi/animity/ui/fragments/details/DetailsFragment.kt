package com.kl3jvi.animity.ui.fragments.details

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.PopupMenu
import androidx.annotation.MenuRes
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import coil.load
import com.google.android.material.chip.Chip
import com.kl3jvi.animity.R
import com.kl3jvi.animity.data.model.ui_models.GenreModel
import com.kl3jvi.animity.databinding.FragmentDetailsBinding
import com.kl3jvi.animity.episodeList
import com.kl3jvi.animity.ui.activities.main.MainActivity
import com.kl3jvi.animity.ui.activities.player.PlayerActivity
import com.kl3jvi.animity.ui.base.BaseFragment
import com.kl3jvi.animity.ui.fragments.favorites.FavoritesViewModel
import com.kl3jvi.animity.utils.*
import com.kl3jvi.animity.utils.Constants.Companion.getBackgroundColor
import com.kl3jvi.animity.utils.Constants.Companion.getColor
import com.kl3jvi.animity.utils.Constants.Companion.showSnack
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class DetailsFragment : BaseFragment<DetailsViewModel, FragmentDetailsBinding>() {

    override val viewModel: DetailsViewModel by viewModels()
    val favoritesViewModel: FavoritesViewModel by activityViewModels()
    private val args: DetailsFragmentArgs by navArgs()
    private val animeDetails get() = args.animeDetails

    //    private val episodeAdapter by lazy { CustomEpisodeAdapter(this, animeDetails.title) }
    private lateinit var menu: Menu
    private lateinit var title: String
    private var check = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun observeViewModel() {
        fetchAnimeInfo()
        fetchEpisodeList()
        showLatestEpisodeReleaseTime()
        animeDetails.let { animeInfo ->
            viewModel.animeMetaModel.value = animeInfo
            binding.apply {
                detailsPoster.load(animeInfo.imageUrl) { crossfade(true) }
                resultTitle.text = animeInfo.title
                title = animeInfo.title
            }
            animeInfo.categoryUrl?.let { url ->
                viewModel.passUrl(url)
            }
        }
    }

    override fun initViews() {

    }


    private fun fetchAnimeInfo() {
        collectFlow(viewModel.animeInfo) { res ->
            when (res) {
                is Resource.Success -> {
                    res.data?.let { info ->
                        binding.animeInfoLayout.textOverview.text = info.plotSummary
                        binding.releaseDate.text = info.releasedTime
                        binding.status.text = info.status
                        binding.type.text = info.type

                        binding.animeInfoLayout.textOverview.visibility = VISIBLE
                        binding.releaseDate.visibility = VISIBLE
                        binding.status.visibility = VISIBLE
                        binding.type.visibility = VISIBLE
                        binding.detailsProgress.visibility = GONE

                        // Check if the type is movie and this makes invisible the listview of the episodes
                        showButtonForMovie(info.type == " Movie")
                        createGenreChips(info.genre)
                    }
                }
                is Resource.Loading -> {
                    binding.animeInfoLayout.textOverview.visibility = GONE
                    binding.releaseDate.visibility = GONE
                    binding.status.visibility = GONE
                    binding.type.visibility = GONE
                }
                is Resource.Error -> {
                    showSnack(binding.root, res.message)
                }
                null -> {}
            }
        }
    }

    private fun showButtonForMovie(isMovie: Boolean) {
        if (isMovie) {
            binding.apply {
                resultEpisodesText.visibility = GONE
                binding.episodeListRecycler.visibility = GONE
                resultPlayMovie.visibility = VISIBLE
                imageButton.visibility = GONE
            }
        } else {
            binding.apply {
                resultEpisodesText.visibility = VISIBLE
                resultPlayMovie.visibility = GONE
                episodeListRecycler.visibility = VISIBLE
            }
        }
    }

    private fun createGenreChips(genre: ArrayList<GenreModel>) {
        genre.forEach { data ->
            binding.genreGroup.removeAllViews()
            val chip = Chip(requireContext())
            chip.apply {
                text = data.genreName
                setTextColor(Color.WHITE)
                chipStrokeColor = getColor()
                chipStrokeWidth = 3f
                chipBackgroundColor = getBackgroundColor()
            }
            binding.genreGroup.addView(chip)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.favorite_menu, menu)
        this.menu = menu
        observeDatabase()
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun observeDatabase() {
        if (isGuestLogin()) {
            viewModel.isOnDatabase.observe(viewLifecycleOwner) {
                check = it
                if (!check) {
                    menu[1].setIcon(R.drawable.ic_favorite_uncomplete)
                } else {
                    menu[1].setIcon(R.drawable.ic_favorite_complete)
                }
            }
        } else {
            collectFlow(favoritesViewModel.favoriteAniListAnimeList) {
                check = it?.data?.user?.favourites?.anime?.edges?.any {
                    it?.node?.id == animeDetails.id || it?.node?.title?.romaji.equals(
                        animeDetails.title,
                        true
                    )
                } ?: false
                if (!check) {
                    menu[1].setIcon(R.drawable.ic_favorite_uncomplete)
                } else {
                    menu[1].setIcon(R.drawable.ic_favorite_complete)
                }
            }
        }

        binding.setType.isVisible = !isGuestLogin()
        binding.setType.setOnClickListener { v ->
            showMenu(v, R.menu.popup_menu)
        }
    }

    private fun showMenu(v: View, @MenuRes menuRes: Int) {
        val popup = PopupMenu(requireContext(), v)
        popup.menuInflater.inflate(menuRes, popup.menu)
        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.option_1 -> {
                    binding.setType.text = requireContext().getText(R.string.completed)
                }
                R.id.option_2 -> {
                    binding.setType.text = requireContext().getText(R.string.watching)
                }
                R.id.option_3 -> {
                    binding.setType.text = requireContext().getText(R.string.planning)
                }
            }
            false
        }
        // Show the popup menu.
        popup.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_to_favorites -> {
                check = if (!check) {
                    if (isGuestLogin()) {
                        viewModel.insert(anime = args.animeDetails)
                        menu[1].setIcon(R.drawable.ic_favorite_complete)
                    } else {
                        viewModel.updateAnimeFavorite()
                        menu[1].setIcon(R.drawable.ic_favorite_complete)
                    }
                    showSnack(binding.root, "Anime added to Favorites")
                    true
                } else {
                    if (isGuestLogin()) {
                        viewModel.delete(anime = args.animeDetails)
                        menu[1].setIcon(R.drawable.ic_favorite_uncomplete)
                    } else {
                        viewModel.updateAnimeFavorite()
                        menu[1].setIcon(R.drawable.ic_favorite_uncomplete)
                    }
                    showSnack(binding.root, "Anime removed from Favorites")
                    false
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @ExperimentalCoroutinesApi
    private fun fetchEpisodeList() {
        collectFlow(viewModel.episodeList) { episodeListResponse ->
            episodeListResponse?.data?.let { episodeList ->
                binding.detailsProgress.visibility = GONE
                binding.episodeListRecycler.withModels {
                    logMessage(episodeList.toString())
                    episodeList.forEach {
                        episodeList {
                            id(it.episodeNumber.hashCode())
                            clickListener { _ ->
                                requireContext().launchActivity<PlayerActivity> {
                                    putExtra(Constants.EPISODE_DETAILS, it)
                                    putExtra(Constants.ANIME_TITLE, animeDetails.title)
                                }
                            }
                            episodeInfo(it)
                        }
                    }
                }
                binding.resultEpisodesText.text =
                    requireContext().getString(R.string.total_episodes, episodeList.size.toString())
                var check = false
                binding.imageButton.setOnClickListener {
                    check = if (!check) {
                        binding.imageButton.load(R.drawable.ic_up_arrow) {
                            crossfade(true)
                        }
//                        episodeAdapter.submitList(episodeList)
                        true
                    } else {
                        binding.imageButton.load(R.drawable.ic_down_arrow) {
                            crossfade(true)
                        }
//                        episodeAdapter.submitList(episodeList.reversed())
                        false
                    }
                }
                if (episodeList.isNotEmpty()) {
                    binding.resultPlayMovie.setOnClickListener {
                        requireActivity().launchActivity<PlayerActivity> {
                            putExtra(
                                Constants.EPISODE_DETAILS,
                                episodeList.first()
                            )
                            putExtra(Constants.ANIME_TITLE, title)
                        }
                        binding.resultPlayMovie.visibility = VISIBLE
                    }
                } else {
                    binding.resultPlayMovie.visibility = GONE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (requireActivity() is MainActivity) {
            (activity as MainActivity?)?.hideBottomNavBar()
        }
    }

    private fun showLatestEpisodeReleaseTime() {
        viewModel.lastEpisodeReleaseTime.observe(viewLifecycleOwner) { res ->
            when (res) {
                is Resource.Success -> {
                    res.data?.let {
                        if (it.time.isNotEmpty()) {
                            binding.nextEpisodeContainer.visibility = VISIBLE
                            binding.releaseTime.text = " ${it.time}"
                        } else binding.nextEpisodeContainer.visibility = GONE
                    }
                }
                is Resource.Error -> {
                    binding.nextEpisodeContainer.visibility = GONE
                }
                is Resource.Loading -> {
                    binding.nextEpisodeContainer.visibility = GONE
                }
            }
        }
    }

    override fun getViewBinding(): FragmentDetailsBinding =
        FragmentDetailsBinding.inflate(layoutInflater)
}

