package jp.winas.android.foundation.scene.uicomponent

import android.content.Context
import androidx.annotation.IdRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

abstract class BaseListView<D, VH : BaseListView.BaseViewHolder>(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {

    constructor(context: Context, attrs: AttributeSet? = null): this(context, attrs, 0)

    init {
        layoutManager = LinearLayoutManager(context, attrs, defStyle, 0)
        adapter = Adapter()
    }

    protected val dataList: MutableList<D> = mutableListOf()
    protected abstract val cellViewResId: Int
    protected abstract fun createViewHolder(itemView: View): VH
    protected open fun onBind(holder: VH, data: D, position: Int) = Unit
    open var onSelect: ((D, Int, VH) -> Unit)? = null

    fun refresh() = adapter?.notifyDataSetChanged()
    fun set(datas: Collection<D>): Boolean = dataList.apply { clear() }.run { addAll(datas) }.also { if (it) refresh() }
    fun add(data: D): Boolean = dataList.add(data).also { if (it) refresh() }
    fun addAll(datas: Collection<D>): Boolean = dataList.addAll(datas).also { if (it) refresh() }
    fun remove(data: D): Boolean = dataList.remove(data).also { if (it) refresh() }
    fun clear() = dataList.clear().also { refresh() }

    open class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun <T : View> findViewById(@IdRes id: Int): T = itemView.findViewById(id)
    }

    inner class Adapter : RecyclerView.Adapter<VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = createViewHolder(
            LayoutInflater.from(parent.context).inflate(cellViewResId, parent, false)
        )

        override fun getItemCount(): Int = dataList.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            onBind(holder, dataList[position], position)
            onSelect?.let { cb -> holder.itemView.setOnClickListener { cb(dataList[position], position, holder) } }
        }
    }
}
