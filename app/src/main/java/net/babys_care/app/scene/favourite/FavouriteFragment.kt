package net.babys_care.app.scene.favourite

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import jp.winas.android.foundation.scene.BaseFragment
import jp.winas.android.foundation.scene.ViewModelable
import kotlinx.android.synthetic.main.fragment_favourite.*
import net.babys_care.app.AppManager
import net.babys_care.app.R
import net.babys_care.app.api.responses.Article
import net.babys_care.app.scene.MainActivity
import net.babys_care.app.scene.article.ArticleDetailFragment
import net.babys_care.app.utils.LoadingHelper
import net.babys_care.app.utils.Toaster
import net.babys_care.app.utils.adapters.ArticleAdapter
import net.babys_care.app.utils.debugLogInfo

class FavouriteFragment : BaseFragment(), ViewModelable<FavouriteViewModel> {

    override val layout = R.layout.fragment_favourite
    override val viewModelClass = FavouriteViewModel::class

    private var loadingDialog: AlertDialog? = null
    val adapter: ArticleAdapter by lazy {
        ArticleAdapter(requireContext()).apply {
            onItemClickListener = {article ->
                transitToArticleDetail(article)
                showDetailView(true)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setRecyclerView()

        getFavouriteArticles()
        getTags()
        getArticleList()

        swipe_refresh.setOnRefreshListener {
            getArticleList(true)
        }
        setupBackPressHandler()
    }

    override fun onResume() {
        (activity as? MainActivity)?.updateToolbarTitle(getString(R.string.favourite))
        super.onResume()
    }

    private fun setRecyclerView() {
        favourite_recycler_view.layoutManager = LinearLayoutManager(requireContext())
        favourite_recycler_view.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        favourite_recycler_view.adapter = adapter
        favourite_recycler_view.setHasFixedSize(true)
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

    private fun getArticleList(isFromSwipe: Boolean = false) {
        if (!isFromSwipe) {
            showLoadingDialog(true)
        }
        viewModel?.fetchArticleList(AppManager.apiToken) { response, error ->
            if (isFromSwipe) {
                swipe_refresh.isRefreshing = false
            } else {
                showLoadingDialog(false)
            }
            if (response != null) {
                filterArticle()
            } else {
                showNoData(false)
                error?.message?.let {
                    Toaster.showToast(it)
                }
            }
        }
    }

    private fun filterArticle() {
        val favouriteArticleIds = viewModel?.favouriteArticleIds ?: mutableListOf()
        if (favouriteArticleIds.isEmpty()) {
            showNoData(false)
        } else {

            val articleList = viewModel?.articleList ?: mutableListOf()
            val filteredArticleList: MutableList<Article> = mutableListOf()
            favouriteArticleIds.sortByDescending { it.created_at }

            for (favourite in favouriteArticleIds) {
                for (article in articleList) {
                    if (article.articleId == favourite.article_id) {
                        article.isFavourite = true
                        filteredArticleList.add(article)
                        break
                    }
                }
            }

            viewModel?.articleList?.clear()
            viewModel?.articleList?.addAll(filteredArticleList)

            if (filteredArticleList.size > 0) {
                adapter.setArticleData(filteredArticleList)
                showNoData(true)
                adapter.notifyDataSetChanged()
            } else {
                showNoData(false)
            }
        }
    }

    private fun getTags() {
        viewModel?.fetchTags(AppManager.apiToken) {isSuccess, error ->
            if (isSuccess) {
                viewModel?.tagList?.let { list ->
                    adapter.setTagData(list)
                    if (viewModel?.articleList?.isNotEmpty() == true) {
                        adapter.notifyDataSetChanged()
                    }
                }
            } else {
                debugLogInfo("Error: $error")
            }
        }
    }

    private fun getFavouriteArticles() {
        viewModel?.fetchFavouriteArticleIds(AppManager.apiToken) { response, error ->
            response?.data?.favorites?.let {
                if (viewModel?.articleList?.size ?: 0 > 0) {
                    filterArticle()
                }
            } ?: kotlin.run {
                debugLogInfo("Error: $error")
                showNoData(false)
            }
        }
    }

    private fun showNoData(isHidden: Boolean) {
        if (isHidden) {
            no_data.visibility = View.GONE
            swipe_refresh.visibility = View.VISIBLE
        } else {
            no_data.visibility = View.VISIBLE
            swipe_refresh.visibility = View.GONE
        }
    }

    fun showDetailView(isDetails: Boolean) {
        if (isDetails) {
            swipe_refresh.visibility = View.GONE
            fragment_container.visibility = View.VISIBLE
        } else {
            fragment_container.visibility = View.GONE
            swipe_refresh.visibility = View.VISIBLE
        }
    }

    private fun transitToArticleDetail(article: Article) {
        val detailFragment = ArticleDetailFragment()
        detailFragment.article = article
        childFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, detailFragment, ArticleDetailFragment::class.java.simpleName)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .addToBackStack(ArticleDetailFragment::class.java.simpleName)
            .commit()
    }

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner) {
                val fragment = childFragmentManager.findFragmentByTag(ArticleDetailFragment::class.java.simpleName)
                if(fragment != null) {
                    childFragmentManager.popBackStack()
                } else {
                    if (isEnabled) {
                        isEnabled = false
                        activity?.onBackPressed()
                    }
                }
            }
    }

    override fun onDestroy() {
        if (loadingDialog != null) {
            loadingDialog?.dismiss()
            loadingDialog = null
        }
        super.onDestroy()
    }
}