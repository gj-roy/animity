package com.kl3jvi.animity.ui.fragments.details

import android.util.Log
import androidx.lifecycle.*
import com.kl3jvi.animity.data.model.ui_models.AnimeMetaModel
import com.kl3jvi.animity.domain.use_cases.GetAnimeDetailsUseCase
import com.kl3jvi.animity.domain.use_cases.GetEpisodeInfoUseCase
import com.kl3jvi.animity.persistence.AnimeRepository
import com.kl3jvi.animity.persistence.EpisodeDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val getAnimeDetailsUseCase: GetAnimeDetailsUseCase,
    private val animeRepository: AnimeRepository,
    private val getEpisodeInfoUseCase: GetEpisodeInfoUseCase,
    private val episodeDao: EpisodeDao
) : ViewModel() {

    private val _url = MutableLiveData<String>()
    private val _animeId = MutableLiveData<Int>()
    private val _downloadUrl = MutableLiveData<String>()

    val animeInfo = Transformations.switchMap(_url) { string ->
        getAnimeDetailsUseCase.fetchAnimeInfo(string).asLiveData()
    }

    @ExperimentalCoroutinesApi
    val episodeList = Transformations.switchMap(_url) { list ->
        getAnimeDetailsUseCase.fetchAnimeInfo(list).flatMapLatest { info ->
            getAnimeDetailsUseCase.fetchEpisodeList(
                info.data?.id,
                info.data?.endEpisode,
                info.data?.alias
            ).onEach {
                val response = it.data
                response?.map { episodeModel ->
                    if (episodeDao.isEpisodeOnDatabase(episodeModel.episodeUrl)) {
                        episodeModel.percentage =
                            episodeDao.getEpisodeContent(episodeModel.episodeUrl)
                                .getWatchedPercentage()
                    }
                }
            }
        }.asLiveData(Dispatchers.Default + viewModelScope.coroutineContext)
    }


    @ExperimentalCoroutinesApi
    val downloadEpisodeUrl = Transformations.switchMap(_downloadUrl) { url ->
        getEpisodeInfoUseCase(url).flatMapLatest { episodeInfo ->
            getEpisodeInfoUseCase.fetchM3U8(episodeInfo.data?.vidCdnUrl)
        }.asLiveData()
    }

    val lastEpisodeReleaseTime = Transformations.switchMap(_url) {
        getAnimeDetailsUseCase.fetchEpisodeReleaseTime(it.split("/").last()).asLiveData()
    }

    val isOnDatabase = Transformations.switchMap(_animeId) { id ->
        getAnimeDetailsUseCase.checkIfExists(id).asLiveData()
    }

    fun passUrl(url: String) {
        _url.value = url
        Log.e("Category URL", url)
    }

    fun passId(id: Int) {
        _animeId.value = id
    }

    fun passDownloadEpisodeUrl(url: String) {
        _downloadUrl.value = url
    }

    fun insert(anime: AnimeMetaModel) = viewModelScope.launch {
        animeRepository.insertFavoriteAnime(anime)
    }

    fun delete(anime: AnimeMetaModel) = viewModelScope.launch {
        animeRepository.deleteAnime(anime)
    }
}
