package net.babys_care.app.scene.about

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.adapter_about_app.view.*
import net.babys_care.app.BuildConfig
import net.babys_care.app.R

class AboutAppAdapter(private val customList: Array<String>) : RecyclerView.Adapter<AboutAppAdapter.CustomViewHolder>() {
    private lateinit var listener: OnItemClickListener

    // ViewHolderクラス(別ファイルに書いてもOK)
    class CustomViewHolder(val view: View): RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val item = layoutInflater.inflate(R.layout.adapter_about_app, parent, false)
        return CustomViewHolder(item)
    }

    override fun getItemCount(): Int {
        return customList.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        if (position == customList.lastIndex) {
            holder.view.about_app_text.text ="${customList[position]} ${BuildConfig.VERSION_NAME}"
            holder.view.open_browser.visibility = View.INVISIBLE
        } else {
            holder.view.about_app_text.text = customList[position]
            holder.view.open_browser.visibility = View.VISIBLE
        }
        holder.view.setOnClickListener {
            listener.onItemClickListener(it, position, customList[position])
        }
    }

    interface OnItemClickListener{
        fun onItemClickListener(view: View, position: Int, clickedText: String)
    }

    fun setOnItemClickListener(listener: OnItemClickListener){
        this.listener = listener
    }
}
