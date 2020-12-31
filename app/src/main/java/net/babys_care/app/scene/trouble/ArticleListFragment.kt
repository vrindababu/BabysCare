package net.babys_care.app.scene.trouble

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import jp.winas.android.foundation.scene.BaseFragment
import jp.winas.android.foundation.scene.ViewModelable
import kotlinx.android.synthetic.main.fragment_article_list.*
import net.babys_care.app.AppManager
import net.babys_care.app.R
import net.babys_care.app.scene.article.ArticleDetailFragment
import net.babys_care.app.utils.DialogHelper
import net.babys_care.app.utils.LoadingHelper
import net.babys_care.app.utils.Toaster
import net.babys_care.app.utils.adapters.ArticleAdapter
import net.babys_care.app.utils.debugLogInfo

class ArticleListFragment : BaseFragment(), ViewModelable<ArticleListViewModel> {

    override val layout = R.layout.fragment_article_list
    override val viewModelClass = ArticleListViewModel::class

    private val adapter by lazy {
        ArticleAdapter(requireContext()).apply {
            onItemClickListener = {article ->
                val fragment = ArticleDetailFragment()
                fragment.article = article
                onArticleClick?.invoke(fragment)
            }

            viewModel?.tagList?.let {list ->
                if (list.isNotEmpty()) {
                    setTagData(list)
                }
            }
        }
    }
    private var loadingDialog: AlertDialog? = null
    var onArticleClick: ((Fragment) -> Unit)? = null
    var parentId: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setRecyclerView()
        getTags()
        getArticleList()
        swipe_refresh.setOnRefreshListener {
            getArticleList(true)
        }
    }

    private fun setRecyclerView() {
        article_recycler_view.layoutManager = LinearLayoutManager(requireContext())
        article_recycler_view.setHasFixedSize(true)
        article_recycler_view.adapter = adapter
        article_recycler_view.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
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
        viewModel?.fetchArticleList(AppManager.apiToken, parentId) { isSuccess, error ->
            if (isFromSwipe) {
                swipe_refresh.isRefreshing = false
            } else {
                showLoadingDialog(false)
            }
            if (isSuccess) {
                article_recycler_view.adapter?.notifyDataSetChanged()
                viewModel?.articleList?.let { list ->
                    if (list.isNotEmpty()) {
                        adapter.setArticleData(list)
                        adapter.notifyDataSetChanged()
                    }
                }
            } else {
                error?.message?.let {
                    Toaster.showToast(it)
                }
            }
            getFavouriteArticles()
        }
    }

    private fun getTags() {
        viewModel?.fetchTags(AppManager.apiToken) {list, errorResponse ->
            if (list != null) {
                if (list.isNotEmpty()) {
                    adapter.setTagData(list)
                    adapter.notifyDataSetChanged()
                }
            } else {
                debugLogInfo("Response error: $errorResponse")
            }
        }
    }

    private fun getFavouriteArticles() {
        viewModel?.getFavouriteArticles(AppManager.apiToken) { response, error ->
            if (AppManager.isLoggedIn) {
                if (response != null) {
                    if (response.data.favorites.isNotEmpty()) {
                        viewModel?.articleList?.let { list ->
                            adapter.setArticleData(list)
                            adapter.notifyDataSetChanged()
                        }
                    }
                } else {
                    debugLogInfo("Error: $error")
                }
            } else {
                activity?.let { activity ->
                    DialogHelper().showLoginTransitionDialog(activity, getString(R.string.authentication_error_ea002), false)
                }
            }
        }
    }

    override fun onDestroyView() {
        if (loadingDialog != null) {
            loadingDialog?.dismiss()
            loadingDialog = null
        }
        super.onDestroyView()
    }
}