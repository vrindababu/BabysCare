package net.babys_care.app.scene.article

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.realm.Realm
import jp.winas.android.foundation.scene.BaseFragment
import jp.winas.android.foundation.scene.ViewModelable
import kotlinx.android.synthetic.main.fragment_article_top.*
import net.babys_care.app.AppManager
import net.babys_care.app.R
import net.babys_care.app.api.responses.Article
import net.babys_care.app.api.responses.FavouriteArticle
import net.babys_care.app.api.responses.SearchHistory
import net.babys_care.app.api.responses.Tag
import net.babys_care.app.extensions.hideKeyBoard
import net.babys_care.app.extensions.toDotDateFormat
import net.babys_care.app.models.realmmodels.ChildrenModel
import net.babys_care.app.models.realmmodels.UserModel
import net.babys_care.app.scene.MainActivity
import net.babys_care.app.utils.EndlessScrollListener
import net.babys_care.app.utils.LoadingHelper
import net.babys_care.app.utils.Toaster.showToast
import net.babys_care.app.utils.debugLogInfo
import java.text.SimpleDateFormat
import java.util.*

private const val ITEM_TYPE_HEADER: Int = 0
private const val ITEM_TYPE_ITEM: Int = 1

class ArticleTopFragment : BaseFragment(), ViewModelable<ArticleTopViewModel> {

    override val layout = R.layout.fragment_article_top
    override val viewModelClass = ArticleTopViewModel::class

    private var loadingDialog: AlertDialog? = null
    private val searchAdapter by lazy {
        SearchHistoryAdapter(requireContext()).apply {
            onClearAllClick = {
                viewModel?.searchHistories?.firstOrNull()?.let {history ->
                    viewModel?.deleteSearchHistory(AppManager.apiToken, history.search_history_id, 1) {response, error ->
                        if (response != null) {
                            this.notifyDataSetChanged()
                        } else {
                            error?.message?.let {
                                showToast(it)
                            }
                        }
                    }
                }
            }

            onClearHistoryClick = {history ->
                viewModel?.deleteSearchHistory(AppManager.apiToken, history.search_history_id) {response, error ->
                    if (response != null) {
                        this.notifyDataSetChanged()
                    } else {
                        error?.message?.let {
                            showToast(it)
                        }
                    }
                }
            }

            onHistoryItemClick = {history ->
                hideKeyBoard()
                getArticleList(searchWord = history.word)
            }
        }
    }
    private var pageNo: Int = 0
    private lateinit var scrollListener: EndlessScrollListener

    override fun onResume() {
        (activity as? MainActivity)?.updateToolbarTitle(getString(R.string.read_article))
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        pageNo = 1
        viewModel?.isLastPage = false
        setRecyclerView()

        getTags()
        getFavouriteArticles()
        getSearchHistories()
        getArticleList()

        setupSearchInputObserver()

        clear_input_button.setOnClickListener {
            search_input_box.text = null
            getArticleList()
        }

        setKeyboardObserver(view)

        swipe_refresh.setOnRefreshListener {
            viewModel?.isLastPage = false
            scrollListener.resetValues()
            getArticleList(true)
        }
    }

    private fun setRecyclerView() {
        //Article recyclerView
        val layoutManager = LinearLayoutManager(requireContext())
        article_recycler_view.layoutManager = layoutManager
        article_recycler_view.setHasFixedSize(true)
        article_recycler_view.adapter = ArticleAdapter(requireContext()).apply {
            onItemClickListener = {article ->
                val fragment = ArticleDetailFragment()
                fragment.article = article
                (parentFragment as? ArticleFragment)?.addFragment(fragment, true)
            }
        }
        article_recycler_view.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        scrollListener = object : EndlessScrollListener(layoutManager) {
            override fun onLoadMore(currentPage: Int) {
                if (viewModel?.articleList?.isNotEmpty() == true && viewModel?.isLastPage == false) {
                    pageNo = currentPage
                    swipe_refresh.isRefreshing = true
                    getArticleList(true, pageNo = currentPage)
                }
            }
        }
        article_recycler_view.addOnScrollListener(scrollListener)

        //Search history recyclerView
        search_history_recycler.layoutManager = LinearLayoutManager(requireContext())
        search_history_recycler.adapter = searchAdapter
    }

    private fun setKeyboardObserver(view: View) {
        view.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            view.getWindowVisibleDisplayFrame(rect)
            val heightDiff = view.rootView.height - (rect.bottom - rect.top)
            if (heightDiff > 500) {
                //Keyboard is visible
                toggleSearchHistoryVisibility(true)
            } else {
                //Keyboard is hidden
                if (search_input_box == null) {
                    return@addOnGlobalLayoutListener
                }
                if (search_input_box.hasFocus()) {
                    search_input_box.clearFocus()
                }
                toggleSearchHistoryVisibility(false)
            }
        }
    }

    private fun setupSearchInputObserver() {
        search_input_box.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                search_input_box.error = null
                if (s == null || s.isEmpty()) {
                    clear_input_button.visibility = View.GONE
                } else {
                    clear_input_button.visibility = View.VISIBLE
                }
            }

            override fun afterTextChanged(s: Editable?) {
                filterSearchHistory(s.toString())
            }
        })

        search_input_box.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                validateInput()
                true
            } else {
                false
            }
        }
    }

    private fun filterSearchHistory(text: String) {
        val data = viewModel?.searchHistories ?: mutableListOf()
        val temporaryList = mutableListOf<SearchHistory>()
        for (historyData in data) {
            if (historyData.word.contains(text, true)) {
                temporaryList.add(historyData)
            }
        }

        if (temporaryList.isNotEmpty()) {
            searchAdapter.updateData(temporaryList)
        } else {
            searchAdapter.updateData(data)
        }
    }

    private fun validateInput() {
        val searchKeyWord = search_input_box.text.toString().trim()
        if (searchKeyWord.length > 100) {
            search_input_box.error = String.format(getString(R.string.enter_at_most_n_character), 100)
        } else {
            search_input_box.clearFocus()
            executeSearch(searchKeyWord)
        }
    }

    private fun executeSearch(word: String) {
        hideKeyBoard()
        val token = AppManager.apiToken
        viewModel?.createSearchHistory(token, word)
        getArticleList(searchWord = word)
    }

    private fun toggleSearchHistoryVisibility(isVisible: Boolean) {
        if (isVisible) {
            if (search_history_recycler.visibility != View.VISIBLE) {
                swipe_refresh.visibility = View.GONE
                search_history_recycler.visibility = View.VISIBLE
                (activity as? MainActivity)?.updateToolbarTitle(getString(R.string.search_for_article))
            }
        } else {
            if (search_history_recycler.visibility != View.GONE) {
                search_history_recycler.visibility = View.GONE
                swipe_refresh.visibility = View.VISIBLE
                (activity as? MainActivity)?.updateToolbarTitle(getString(R.string.read_article))
            }
        }
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

    private fun getArticleList(isFromSwipe: Boolean = false, pageNo: Int = 1, searchWord: String? = null) {
        if (!isFromSwipe) {
            showLoadingDialog(true)
        }
        viewModel?.fetchArticleList(AppManager.apiToken, pageNo, searchWord) {response, error ->
            if (context == null || swipe_refresh == null) return@fetchArticleList
            if (isFromSwipe) {
                swipe_refresh.isRefreshing = false
            } else {
                showLoadingDialog(false)
            }
            if (response != null) {
                if (response.data.articles.size < 100) {
                    viewModel?.isLastPage = true
                }
                if (pageNo == 1) {
                    this.pageNo = pageNo
                    viewModel?.articleList?.clear()
                }
                filterArticle(response.data.articles)
            } else {
                error?.message?.let {
                    showToast(it)
                }
            }
        }
    }

    private fun filterArticle(articles: List<Article>) {
        val realm = Realm.getDefaultInstance()
        val filteredArticleList = mutableListOf<Article>()
        if (isPreMama(realm)) {
            val tagId = 56
            filteredArticleList.addAll(articles.filter { it.tagIds.contains(tagId) }.toMutableList())
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

                for (article in articles) {
                    for (tagId in tagIdList) {
                        val item = article.tagIds.firstOrNull { it == tagId }
                        if (item != null) {
                            filteredArticleList.add(article)
                            break
                        }
                    }
                }
            }
            realm.close()
        }

        if (filteredArticleList.isEmpty()) {
            if (viewModel?.isLastPage == false) {
                pageNo++
                scrollListener.currentPage = pageNo
                swipe_refresh.isRefreshing = true
                getArticleList(true, pageNo = pageNo)
            }
            if (viewModel?.articleList?.isNullOrEmpty() == true) {
                no_corresponding_article.visibility = View.VISIBLE
            }
            return
        }

        val favouriteArticleIds = viewModel?.favouriteArticleIds ?: mutableListOf()
        if (filteredArticleList.isEmpty() && viewModel?.articleList?.isNullOrEmpty() == true) {
            no_corresponding_article.visibility = View.VISIBLE
        } else {
            no_corresponding_article.visibility = View.GONE
            if (favouriteArticleIds.size > 0) {
                for (favourite in favouriteArticleIds) {
                    for (article in filteredArticleList) {
                        if (article.articleId == favourite.article_id) {
                            article.isFavourite = true
                            break
                        }
                    }
                }
            }
            filteredArticleList.sortByDescending {
                val formatter = if (it.date.contains("/")) {
                    SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
                } else {
                    SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN)
                }
                formatter.parse(it.date)
            }
            val previousSize: Int = viewModel?.articleList?.size ?: 0
            viewModel?.articleList?.addAll(filteredArticleList)
            debugLogInfo("Filtered size: ${viewModel?.articleList?.size ?: 0}")
            if (pageNo == 1) {
                article_recycler_view.adapter?.notifyDataSetChanged()
            } else {
                article_recycler_view.adapter?.notifyItemRangeInserted(
                    previousSize,
                    filteredArticleList.size
                )
            }
        }
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

    private fun getTags() {
        viewModel?.fetchTags(AppManager.apiToken)
    }

    private fun getSearchHistories() {
        viewModel?.fetchSearchHistory(AppManager.apiToken) { _, error ->
            if (context == null || swipe_refresh == null) return@fetchSearchHistory
            viewModel?.searchHistories?.let {list ->
                searchAdapter.updateData(list)
                searchAdapter.notifyDataSetChanged()
            }
            error?.message?.let { message ->
                debugLogInfo("Error message: $message")
            }
        }
    }

    private fun getFavouriteArticles() {
        viewModel?.fetchFavouriteArticleIds(AppManager.apiToken) { response, error ->
            if (context == null || swipe_refresh == null) return@fetchFavouriteArticleIds
            response?.data?.favorites?.let { list ->
                if (viewModel?.articleList?.size ?: 0 > 0) {
                    setFavouriteArticle(list)
                    article_recycler_view.adapter?.notifyDataSetChanged()
                }
            } ?: kotlin.run {
                debugLogInfo("Error: $error")
            }
        }
    }

    private fun setFavouriteArticle(list: List<FavouriteArticle>) {
        val articleList = viewModel?.articleList ?: return
        for (favourite in list) {
            for (article in articleList) {
                if (article.articleId == favourite.article_id) {
                    article.isFavourite = true
                    break
                }
            }
        }
    }

    override fun onDestroyView() {
        view?.viewTreeObserver?.removeOnGlobalLayoutListener {}
        if (loadingDialog != null) {
            loadingDialog?.dismiss()
            loadingDialog = null
        }
        super.onDestroyView()
    }

    inner class SearchHistoryAdapter(val context: Context) : RecyclerView.Adapter<SearchHistoryViewHolder>() {

        var onClearAllClick: (() -> Unit)? = null
        var onClearHistoryClick: ((SearchHistory) -> Unit)? = null
        var onHistoryItemClick: ((SearchHistory) -> Unit)? = null
        private var searchHistories: List<SearchHistory> = listOf()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchHistoryViewHolder {
            return if (viewType == ITEM_TYPE_HEADER) {
                SearchHistoryHeaderViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.header_search_history_recycler, parent, false))
            } else {
                SearchHistoryItemViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_search_history_recycler, parent, false))
            }
        }

        override fun onBindViewHolder(holder: SearchHistoryViewHolder, position: Int) {
            if (position == 0) {
                val header = holder as SearchHistoryHeaderViewHolder
                if (searchHistories.isNotEmpty()) {
                    header.clearAll.setTextColor(ContextCompat.getColor(context, R.color.azure))
                    header.clearAll.setOnClickListener {
                        onClearAllClick?.invoke()
                    }
                } else {
                    header.clearAll.setTextColor(ContextCompat.getColor(context, R.color.pinkishGrey))
                }
            } else {
                val itemHolder = holder as SearchHistoryItemViewHolder
                if (searchHistories.isEmpty()) {
                    itemHolder.historyText.text = getString(R.string.no_search_history)
                    itemHolder.clearButton.visibility = View.INVISIBLE
                } else {
                    val historyItem = searchHistories[position - 1]
                    itemHolder.historyText.text = historyItem.word
                    itemHolder.historyText.setOnClickListener {
                        onHistoryItemClick?.invoke(historyItem)
                    }
                    itemHolder.clearButton.visibility = View.VISIBLE
                    itemHolder.clearButton.setOnClickListener {
                        onClearHistoryClick?.invoke(historyItem)
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            val size = if (searchHistories.isNotEmpty()) searchHistories.size else 1
            return size + 1
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == 0) {
                ITEM_TYPE_HEADER
            } else {
                ITEM_TYPE_ITEM
            }
        }

        fun updateData(list: List<SearchHistory>) {
            searchHistories = list
        }
    }
    
    open inner class SearchHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class SearchHistoryItemViewHolder(itemView: View) : SearchHistoryViewHolder(itemView) {
        val historyText: TextView = itemView.findViewById(R.id.history_text)
        val clearButton: ImageView = itemView.findViewById(R.id.clear_button)
    }

    inner class SearchHistoryHeaderViewHolder(itemView: View) : SearchHistoryViewHolder(itemView) {
        val clearAll: TextView = itemView.findViewById(R.id.button_clear_all_history)
    }

    inner class ArticleAdapter(val context: Context): RecyclerView.Adapter<ArticleViewHolder>() {

        private val viewPool = RecyclerView.RecycledViewPool()
        var onItemClickListener: ((Article) -> Unit)? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
            return ArticleViewHolder(LayoutInflater.from(context).inflate(R.layout.item_article_recycler_view, parent, false))
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
            val article = viewModel?.articleList?.get(position) ?: return
            holder.title.text = article.title
            holder.viewCount.text = "${article.view}view"
            holder.date.text = article.date.toDotDateFormat()
            if (article.isFavourite) {
                holder.favouriteIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_favorite_filled_24))
            } else {
                holder.favouriteIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_favorite_24))
            }
            Glide.with(context).load(article.sourceUrl).into(holder.image)
            if (article.tagIds.isNotEmpty()) {
                setTag(holder, article, viewPool)
            } else {
                holder.tagRecyclerView.adapter = null
            }

            holder.itemView.setOnClickListener {
                onItemClickListener?.invoke(article)
            }
        }

        private fun setTag(
            holder: ArticleViewHolder,
            article: Article,
            viewPool: RecyclerView.RecycledViewPool
        ) {
            var threshHold = 15
            val tagList = viewModel?.tagList
            if (tagList?.isNotEmpty() == true) {
                val tags = mutableListOf<Tag>()
                for (tagId in article.tagIds) {
                    for (tagItem in tagList) {
                        if (tagItem.tagId == tagId && tagItem.name.length < threshHold) {
                            tags.add(tagItem)
                            threshHold -= (tagItem.name.length + 2)
                            break
                        }
                    }
                }

                holder.tagRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                holder.tagRecyclerView.adapter = TagAdapter((tags))
                holder.tagRecyclerView.setRecycledViewPool(viewPool)
            }
        }

        override fun getItemCount(): Int {
            return viewModel?.articleList?.size ?: 0
        }
    }

    inner class TagAdapter(private val tagList: List<Tag>) : RecyclerView.Adapter<TagViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
            return TagViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_tag_recycler, parent, false))
        }

        override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
            val tag = tagList[position]
            holder.tagName.text = tag.name
        }

        override fun getItemCount(): Int {
            return tagList.size
        }

    }

    inner class ArticleViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.article_image)
        val title: TextView = itemView.findViewById(R.id.article_title)
        val tagRecyclerView: RecyclerView = itemView.findViewById(R.id.tag_recycler_view)
        val viewCount: TextView = itemView.findViewById(R.id.article_view_count)
        val favouriteIcon: ImageView = itemView.findViewById(R.id.favourite_icon)
        val date: TextView = itemView.findViewById(R.id.article_date)
    }

    inner class TagViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val tagName: TextView =itemView.findViewById(R.id.article_tag)
    }
}