package net.babys_care.app.utils

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

abstract class EndlessScrollListener(private val layoutManager: LinearLayoutManager?): RecyclerView.OnScrollListener() {

    private var previousTotal = 0
    private var loading = true
    var currentPage = 1

    private var visibleItemCount: Int = -1
    private var totalItemCount: Int = -1
    private var firstVisibleItem: Int = -1

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        if (layoutManager == null) return

        visibleItemCount = recyclerView.childCount
        totalItemCount = layoutManager.itemCount
        firstVisibleItem = layoutManager.findFirstVisibleItemPosition()

        if (loading) {
            if (totalItemCount > previousTotal) {
                loading = false
                previousTotal = totalItemCount
            }
        }

        if (!loading && (totalItemCount - visibleItemCount) <= (firstVisibleItem)) {
            currentPage++
            onLoadMore(currentPage)
            loading = true
        }
    }

    abstract fun onLoadMore(currentPage: Int)

    fun resetValues() {
        previousTotal = 0
        loading = true
        visibleItemCount = -1
        totalItemCount = -1
        firstVisibleItem = -1
        currentPage = 1
    }

}