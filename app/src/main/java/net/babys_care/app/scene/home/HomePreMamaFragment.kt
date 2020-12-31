package net.babys_care.app.scene.home

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import io.realm.Realm
import io.realm.kotlin.where
import jp.winas.android.foundation.scene.BaseFragment
import jp.winas.android.foundation.scene.ViewModelable
import kotlinx.android.synthetic.main.fragment_home_pre_mama.*
import net.babys_care.app.AppManager
import net.babys_care.app.AppSetting
import net.babys_care.app.BuildConfig
import net.babys_care.app.R
import net.babys_care.app.api.responses.Article
import net.babys_care.app.api.responses.Author
import net.babys_care.app.api.responses.FavouriteArticle
import net.babys_care.app.api.responses.Tag
import net.babys_care.app.extensions.toDotDateFormat
import net.babys_care.app.extensions.toMonthDateDayFormat
import net.babys_care.app.models.realmmodels.ChildrenModel
import net.babys_care.app.models.realmmodels.UserModel
import net.babys_care.app.scene.MainActivity
import net.babys_care.app.scene.article.ArticleDetailFragment
import net.babys_care.app.scene.settings.BabyInfoCheckFragment
import net.babys_care.app.scene.settings.UserSettingsFragment
import net.babys_care.app.utils.LoadingHelper
import net.babys_care.app.utils.adapters.ArticleAdapter
import net.babys_care.app.utils.adapters.TagAdapter
import net.babys_care.app.utils.debugLogInfo
import java.text.SimpleDateFormat
import java.util.*

class HomePreMamaFragment : BaseFragment(), ViewModelable<HomePreMamaViewModel> {

    override val layout = R.layout.fragment_home_pre_mama
    override val viewModelClass = HomePreMamaViewModel::class
    private lateinit var adapter: ArticleAdapter
    private var loadingDialog: AlertDialog? = null
    var onFragmentAdd: ((Fragment) -> Unit)? = null
    private var pageNo: Int = 1

    override fun onResume() {
        setTitle()
        when (val activity = activity) {
            is MainActivity -> {
                activity.addBackButtonAndActionToMain(false)
                if (AppSetting(requireContext()).userInfoUpdated == true) {
                    AppSetting(requireContext()).userInfoUpdated = null
                    activity.setNavHeader()
                }
            }
        }
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setRecyclerView()
        reloadScreen()
        swipe_refresh.setOnRefreshListener {
            reloadScreen(true)
        }
        profile_image_change_button.setOnClickListener {
            val settingsFragment = UserSettingsFragment()
            settingsFragment.onFragmentAdd = onFragmentAdd
            onFragmentAdd?.invoke(settingsFragment)
        }
        button_tap_when_born.setOnClickListener {
            val fragment = BabyInfoCheckFragment()
            fragment.onFragmentAdd = onFragmentAdd
            onFragmentAdd?.invoke(fragment)
        }
        button_see_other_article.setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.article)
        }
        setUserData()
    }

    private fun reloadScreen(isFromSwipe: Boolean = false) {
        pageNo = 1
        getTags()
        getFavouriteArticles()
        getAuthorData()
        getArticles(isFromSwipe, pageNo)
    }

    private fun setUserData() {
        val realm = Realm.getDefaultInstance()
        val user = realm.where<UserModel>().findFirst() ?: return
        display_name.text = user.firstNameKana
        val defaultImage = when(user.gender) {
            "male" -> ContextCompat.getDrawable(requireContext(), R.drawable.default_profile_male)
            else -> ContextCompat.getDrawable(requireContext(), R.drawable.default_profile_female)
        }
        user.image?.let {
            Glide.with(requireContext()).load("${BuildConfig.URL_IMAGE_DIRECTORY}$it").apply(RequestOptions().placeholder(defaultImage)).into(profile_image)
        } ?: kotlin.run {
            profile_image.setImageDrawable(defaultImage)
        }
    }

    private fun getTags() {
        viewModel?.fetchTags(AppManager.apiToken) { list, error ->
            if (context == null || article_container == null) return@fetchTags
            if (error != null) {
                debugLogInfo("Error: $error")
            } else {
                adapter.setTagData(list)
                if (viewModel?.articleList?.isNotEmpty() == true) {
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun getArticles(isFromSwipe: Boolean = false, pageNo: Int = 1, isPaging: Boolean = false) {
        if (!isPaging && !isFromSwipe) {
            showLoadingDialog(true)
        }
        viewModel?.fetchArticleList(AppManager.apiToken, pageNo) { response, error ->
            if (context == null || article_container == null) return@fetchArticleList
            if (response != null) {
                if (pageNo == 1) {
                    viewModel?.articleList?.clear()
                }
                filterArticle(response.data.articles, isFromSwipe)
            } else {
                if (isFromSwipe) {
                    swipe_refresh.isRefreshing = false
                } else {
                    showLoadingDialog(false)
                }
                debugLogInfo("Error: $error")
            }
        }
    }

    private fun getFavouriteArticles() {
        viewModel?.fetchFavouriteArticleIds(AppManager.apiToken) { response, error ->
            if (context == null || article_container == null) return@fetchFavouriteArticleIds
            response?.data?.favorites?.let { list ->
                if (viewModel?.articleList?.size ?: 0 > 0) {
                    setFavouriteArticle(list)
                }
            } ?: kotlin.run {
                debugLogInfo("Error: $error")
            }
        }
    }

    private fun setFavouriteArticle(list: List<FavouriteArticle>) {
        val articleList = viewModel?.articleList ?: return
        for (favouriteArticle in list) {
            for (article in articleList) {
                if (article.articleId == favouriteArticle.article_id) {
                    article.isFavourite = true
                    break
                }
            }
        }

        resizeAndDisplayArticleList()
    }

    private fun getAuthorData() {
        viewModel?.fetchAuthor(AppManager.apiToken) { response, error ->
            if (context == null || article_container == null) return@fetchAuthor
            if (response != null) {
                if (viewModel?.articleList?.isNullOrEmpty() == false) {
                    setAuthorData(response.data.authors)
                }
            } else {
                debugLogInfo("Error: $error")
            }
        }
    }

    private fun setAuthorData(list: List<Author>) {
        if (viewModel?.articleList == null || viewModel?.articleList?.isEmpty() == true) {
            return
        }
        val article = viewModel?.articleList?.get(0)
        for (author in list) {
            if (author.authorId == article?.author) {
                writer_name.text = author.name
                Glide.with(requireContext()).load(author.avatarUrl).into(writer_image)
                break
            }
        }
    }

    private fun filterArticle(articles: List<Article>, isFromSwipe: Boolean) {
        val realm = Realm.getDefaultInstance()
        var threshHold = 5
        threshHold -= viewModel?.articleList?.size ?: 0
        if (isPreMama(realm)) {
            val tagId = 56
            val filteredArticle = articles.filter { it.tagIds.contains(tagId) }
            viewModel?.articleList?.addAll(filteredArticle)
            debugLogInfo("Filtered size: ${viewModel?.articleList?.size ?: 0}")
        } else {
            val childList = realm.where(ChildrenModel::class.java).findAll()
            if (childList.isNotEmpty()) {
                val tagIdList = mutableListOf<Int>()
                for (child in childList) {
                    val tagId = getTagId(child.birthDay)
                    if (!tagIdList.contains(tagId)) {
                        tagIdList.add(tagId)
                    }
                }

                val filteredArticleList = mutableListOf<Article>()
                tagLoop@ for (tagId in tagIdList) {
                    for (article in articles) {
                        if (filteredArticleList.contains(article)) {
                            continue
                        }
                        if (article.tagIds.contains(tagId)) {
                            filteredArticleList.add(article)
                            threshHold--
                            break
                        }
                        if (threshHold < 1) {
                            break@tagLoop
                        }

                    }
                }
                viewModel?.articleList?.addAll(filteredArticleList)
                debugLogInfo("Filtered size: ${viewModel?.articleList?.size ?: 0}")
            }
        }

        if (viewModel?.articleList?.size ?: 0 < 5 && articles.size >= 100) {
            pageNo++
            getArticles(false, pageNo, true)
            return
        }

        if (isFromSwipe) {
            swipe_refresh.isRefreshing = false
        } else {
            showLoadingDialog(false)
        }

        viewModel?.articleList?.sortByDescending {
            val formatter = if (it.date.contains("/")) {
                SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
            } else {
                SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN)
            }
            formatter.parse(it.date)
        }
        val favouriteArticleIds = viewModel?.favouriteArticleIds ?: mutableListOf()
        if (favouriteArticleIds.size > 0) {
            for (favourite in favouriteArticleIds) {
                for (article in articles) {
                    if (article.articleId == favourite.article_id) {
                        article.isFavourite = true
                        break
                    }
                }
            }
        }

        resizeAndDisplayArticleList()

        realm.close()
    }

    private fun isPreMama(realm: Realm): Boolean {
        val  user = realm.where(UserModel::class.java).findFirst()
        if (user?.isPremama == 1) {
            return true
        }

        return false
    }

    private fun getTagId(birthDay: String?): Int {
        if (birthDay == null) {
            return 0
        }

        return when(getBabyAgeInMonth(birthDay)) {
            0 -> 82 //Baby age is 0 month
            1 -> 83
            2 -> 84
            3 -> 85
            4 -> 86
            5 -> 87
            6 -> 88
            7 -> 89
            8 -> 90
            9 -> 91
            10 -> 92
            11 -> 93
            12 -> 94
            else -> 0
        }
    }

    private fun getBabyAgeInMonth(birthDay: String): Int {
        val birthdayFormat = if (birthDay.contains("/")) "yyyy/MM/dd" else "yyyy-MM-dd"
        try {
            val currentDate = Calendar.getInstance()
            val birthDate = SimpleDateFormat(birthdayFormat, Locale.JAPAN).parse(birthDay) ?: Date()

            val dob = Calendar.getInstance()
            dob.time = birthDate
            val age = currentDate.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
            if (age > 1 || age < 0) {
                return -1
            }
            if (age == 0 && (currentDate.get(Calendar.MONTH) - dob.get(Calendar.MONTH)) == 0) {
                //Age is 0 month
                return 0
            }

            return if (age == 1) {
                val diff = (currentDate.get(Calendar.MONTH) - dob.get(Calendar.MONTH))
                if (diff == 0) {
                    if (currentDate.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                        11
                    } else {
                        12
                    }
                } else {
                    12 + diff
                }

            } else {
                currentDate.get(Calendar.MONTH) - dob.get(Calendar.MONTH)
            }
        } catch (ex: Exception) {
            debugLogInfo("Exception: $ex")
        }

        return -1
    }

    private fun setRecyclerView() {
        //Recommended Article recyclerView
        article_recycler_view.layoutManager = LinearLayoutManager(requireContext())
        adapter = ArticleAdapter(requireContext()).apply {
            onItemClickListener = { article ->
                transitToArticleDetails(article)
            }
        }
        article_recycler_view.adapter = adapter
        article_recycler_view.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )
    }

    @SuppressLint("SetTextI18n")
    private fun setArticleData(article: Article) {
        setArticleVisibility(true)
        article_title.text = article.title
        article_content.text = getStrippedContent(article)
        Glide.with(requireContext()).load(article.sourceUrl).into(article_image)
        view_count.text = "${article.view}view"
        article_date.text = article.date.toDotDateFormat()
        if (article.isFavourite) {
            favourite_icon.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_favorite_filled_24
                )
            )
        } else {
            favourite_icon.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_favorite_24
                )
            )
        }
        viewModel?.authorList?.let { list ->
            setAuthorData(list)
        }
        setTagsForArticleDetail(article)
    }

    private fun setTagsForArticleDetail(article: Article) {
        val tags = viewModel?.tagList ?: return
        val tagList = mutableListOf<Tag>()
        for (tagId in article.tagIds) {
            for (tag in tags) {
                if (tagId == tag.tagId) {
                    tagList.add(tag)
                    break
                }
            }
        }
        val layoutManager = FlexboxLayoutManager(context)
        layoutManager.flexDirection = FlexDirection.ROW
        layoutManager.justifyContent = JustifyContent.FLEX_START
        tag_recycler_view.layoutManager = layoutManager
        tag_recycler_view.adapter = TagAdapter(tagList)
    }

    private fun getStrippedContent(article: Article): String {
        return article.content.replace(Regex("(<.*?>)|(&.*?;)|([ ]{2,})"), "")
    }

    private fun setArticleVisibility(isVisible: Boolean) {
        if (isVisible) {
            article_container.visibility = View.VISIBLE
            article_container.setOnClickListener {
                viewModel?.articleList?.get(0)?.let { article ->
                    transitToArticleDetails(article)
                }
            }
        } else {
            article_container.setOnClickListener(null)
            article_container.visibility = View.GONE
        }
    }

    private fun resizeAndDisplayArticleList() {
        val articleList = viewModel?.articleList ?: listOf()
        when {
            articleList.isEmpty() -> {
                setArticleVisibility(false)
                adapter.setArticleData(listOf())
            }
            articleList.size == 1 -> {
                setArticleData(articleList[0])
                adapter.setArticleData(listOf())
            }
            articleList.size > 5 -> {
                setArticleData(articleList[0])
                adapter.setArticleData(articleList.subList(1, 5))
            }
            articleList.isNotEmpty() -> {
                setArticleData(articleList[0])
                adapter.setArticleData(articleList.subList(1, articleList.size))
            }
        }

        adapter.notifyDataSetChanged()
    }

    private fun showLoadingDialog(isShow: Boolean) {
        if (loadingDialog != null) {
            loadingDialog?.dismiss()
            loadingDialog = null
        }

        if (isShow) {
            loadingDialog = LoadingHelper().getLoadingDialog(requireContext())
            loadingDialog?.show()
        }
    }

    private fun setTitle() {
        val date = Date()
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
        dateFormat.format(date).toMonthDateDayFormat()?.let {
            (activity as? MainActivity)?.updateToolbarTitle("今日$it")
        }
    }

    private fun transitToArticleDetails(article: Article) {
        val detailFragment = ArticleDetailFragment()
        detailFragment.article = article
        (parentFragment as? HomeFragment)?.showFragment(detailFragment)
    }

    override fun onDestroyView() {
        if (loadingDialog != null) {
            loadingDialog?.dismiss()
            loadingDialog = null
        }
        super.onDestroyView()
    }
}