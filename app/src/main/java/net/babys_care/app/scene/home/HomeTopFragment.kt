package net.babys_care.app.scene.home

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.AssetManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import io.realm.Realm
import io.realm.Sort
import io.realm.kotlin.where
import jp.winas.android.foundation.scene.BaseFragment
import jp.winas.android.foundation.scene.ViewModelable
import kotlinx.android.synthetic.main.fragment_home_top.*
import net.babys_care.app.AppManager
import net.babys_care.app.AppSetting
import net.babys_care.app.R
import net.babys_care.app.api.responses.Article
import net.babys_care.app.api.responses.Author
import net.babys_care.app.api.responses.FavouriteArticle
import net.babys_care.app.api.responses.Tag
import net.babys_care.app.constants.AppConstants
import net.babys_care.app.extensions.*
import net.babys_care.app.models.*
import net.babys_care.app.models.realmmodels.ChildrenModel
import net.babys_care.app.models.realmmodels.GrowthHistories
import net.babys_care.app.models.realmmodels.UserModel
import net.babys_care.app.scene.MainActivity
import net.babys_care.app.scene.article.ArticleDetailFragment
import net.babys_care.app.scene.settings.BabyInfoCheckFragment
import net.babys_care.app.utils.DateUtils
import net.babys_care.app.utils.LoadingHelper
import net.babys_care.app.utils.adapters.ArticleAdapter
import net.babys_care.app.utils.adapters.BabyInfoAdapter
import net.babys_care.app.utils.adapters.LegendAdapter
import net.babys_care.app.utils.adapters.TagAdapter
import net.babys_care.app.utils.debugLogInfo
import net.babys_care.app.utils.graph.BabyXAxisValueFormatter
import net.babys_care.app.utils.graph.MyFillFormatter
import net.babys_care.app.utils.graph.MyLineLegendRenderer
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.Calendar.getInstance
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.math.abs

class HomeTopFragment : BaseFragment(), ViewModelable<HomeTopViewModel> {

    override val layout = R.layout.fragment_home_top
    override val viewModelClass = HomeTopViewModel::class
    private lateinit var adapter: ArticleAdapter
    private var loadingDialog: AlertDialog? = null
    private lateinit var realm: Realm
    private lateinit var legendAdapter: LegendAdapter
    var onFragmentAdd: ((Fragment) -> Unit)? = null
    private lateinit var currentDate: Calendar
    private val childGrowthData: MutableList<ChildGrowth> = mutableListOf()
    private var graphSelectedDisplayStyle: GraphDisplayStyle = GraphDisplayStyle.ONE_MONTH
    private lateinit var xAxisLabel: List<String>
    private var backCount: Int = 0
    private var pageNo: Int = 1
    private lateinit var data: LineData
    private var index = 0

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
        realm = Realm.getDefaultInstance()
        setRecyclerView()
        setLegendRecyclerView()
        setBabyInfoRecyclerView()
        date_range_type.setOnClickListener {
            index = 0
            showGraphDisplayTypeSelector()
        }
        date_next_button.setOnClickListener {
            handleDateRangeChangeButtonClick(false)
        }
        date_previous_button.setOnClickListener {
            handleDateRangeChangeButtonClick(true)
        }
        btn_switch.setOnClickListener {
            handleSwitchChildClick()
        }
        graphInitialSetup()
        swipe_refresh.setOnRefreshListener {
            reloadScreen(true)
        }
        button_see_other_article.setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.article)
        }
        reloadScreen()
    }

    private fun handleSwitchChildClick() {
        childGrowthData.let {
            Log.i("index=", "index=" + index)
            if (index >= childGrowthData.size - 1) {
                index = 0
            } else {
                index++
            }
            viewModel?.setBirthday(childGrowthData[index])
            viewModel?.setSelectedChildId(childGrowthData[index])
        }
        filterGraphData(graphSelectedDisplayStyle, true)
    }

    private fun reloadScreen(isFromSwipe: Boolean = false) {
        childGrowthData.clear()
        currentDate = Calendar.getInstance()
        data = LineData()
        data.clearValues()
        backCount = 0
        pageNo = 1
        getTags()
        getFavouriteArticles()
        getAuthorData()
        date_next_button.isEnabled = false
        getArticles(isFromSwipe, pageNo)
        setBabyInfoData()
    }

    private fun graphInitialSetup() {
        line_chart.isScaleXEnabled = false
        line_chart.isDoubleTapToZoomEnabled = false
        line_chart.description.isEnabled = false
        line_chart.legend.isEnabled = false
        line_chart.axisLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
        line_chart.axisLeft.setDrawAxisLine(false)
        line_chart.axisLeft.setDrawGridLines(false)
        line_chart.axisRight.setDrawAxisLine(false)
        line_chart.xAxis.setDrawGridLines(false)
        line_chart.axisRight.setDrawLabels(true)
        line_chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        line_chart.xAxis.setAvoidFirstLastClipping(true)
    }

    private fun getTags() {
        viewModel?.fetchTags(AppManager.apiToken) { list, error ->
            if (swipe_refresh == null) return@fetchTags
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

    private fun getArticles(
        isFromSwipe: Boolean = false,
        pageNo: Int = 1,
        isPaging: Boolean = false
    ) {
        if (!isPaging && !isFromSwipe) {
            showLoadingDialog(true)
        }
        viewModel?.fetchArticleList(AppManager.apiToken, pageNo) { response, error ->
            if (context == null || swipe_refresh == null) {
                return@fetchArticleList
            }
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
            if (swipe_refresh == null) return@fetchFavouriteArticleIds
            if (context == null) {
                return@fetchFavouriteArticleIds
            }
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
        for (favourite in list) {
            for (article in articleList) {
                if (article.articleId == favourite.article_id) {
                    article.isFavourite = true
                    break
                }
            }
        }

        resizeAndDisplayArticleList()
    }

    private fun getAuthorData() {
        viewModel?.fetchAuthor(AppManager.apiToken) { response, error ->
            if (context == null || swipe_refresh == null) {
                return@fetchAuthor
            }
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
        val user = realm.where(UserModel::class.java).findFirst()
        if (user?.isPremama == 1) {
            return true
        }

        return false
    }

    private fun getTagId(birthDay: String?): Int {
        if (birthDay == null) {
            return 0
        }

        return when (getBabyAgeInMonth(birthDay)) {
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

    private fun setBabyInfoRecyclerView() {
        if (baby_info_recycler_view == null) return
        //Baby info recyclerView
        baby_info_recycler_view.layoutManager = LinearLayoutManager(requireContext())
        baby_info_recycler_view.adapter = BabyInfoAdapter().apply {
            onInputLinkClick = { child ->
                val fragment = GrowthDataInputFragment()
                fragment.arguments = Bundle().apply {
                    putInt(AppConstants.CHILD_ID, child.childId)
                }
                onFragmentAdd?.invoke(fragment)
            }

            onProfileSettingClick = {
                val fragment = BabyInfoCheckFragment()
                fragment.onFragmentAdd = onFragmentAdd
                onFragmentAdd?.invoke(fragment)
            }
        }
        baby_info_recycler_view.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )
    }

    fun setBabyInfoData() {
        if (context == null) return
        val data = mutableListOf<ChildGrowth>()
        val children = realm.where<ChildrenModel>().findAll()
        for (child in children) {
            val childGrowth = ChildGrowth(
                child.childId, child.lastName, child.firstName,
                child.lastNameKana, child.firstNameKana, child.image, child.gender,
                child.birthDay, child.birthOrder, child.siblingOrder
            )
            realm.where<GrowthHistories>().equalTo("childId", child.childId).sort(
                "measuredAt",
                Sort.DESCENDING
            ).findFirst()?.let {
                childGrowth.growthHistories.add(it)
            }

            data.add(childGrowth)
        }
        (baby_info_recycler_view.adapter as? BabyInfoAdapter)?.setBabyInfoData(data)
        setTodayWord(children)

        childGrowthData.clear()
        for (child in children) {
            val childGrowth = ChildGrowth(
                child.childId, child.lastName, child.firstName,
                child.lastNameKana, child.firstNameKana, child.image, child.gender,
                child.birthDay, child.birthOrder, child.siblingOrder
            )

            childGrowthData.add(childGrowth)
        }
        Log.i("", "childgrowthdata=" + childGrowthData)
        childGrowthData.sortBy { it.childId }
        childGrowthData.let {
            viewModel?.setBirthday(childGrowthData[0])
            viewModel?.setSelectedChildId(childGrowthData[0])
        }
        filterGraphData(graphSelectedDisplayStyle, true)
    }

    private fun setGraphData(childGrowthData: MutableList<ChildGrowth>, targetDate: Calendar) {
        val filteredData = mutableListOf<ChildGrowth>()
        for (growth in childGrowthData) {
            if (growth.growthHistories.isNotEmpty()) {
                filteredData.add(growth)
            }
            if (filteredData.size == 5) {
                break
            }
        }
        if (filteredData.isEmpty()) {
            line_chart.visibility = View.INVISIBLE
            img_no_data.visibility = View.VISIBLE
            img_average.visibility = View.GONE
            //btn_switch.visibility=View.GONE
            return
        } else {
            line_chart.visibility = View.VISIBLE
            img_no_data.visibility = View.GONE
            img_average.visibility = View.VISIBLE
            //btn_switch.visibility=View.VISIBLE
        }

        setSwicthButtonStatus(filteredData)
        val heightColors = arrayListOf(
            R.color.mango,
            R.color.graph_legend_two_weight,
            R.color.colorAccent,
            R.color.red,
            R.color.black30
        )
        val weightColors = arrayListOf(
            R.color.apple,
            R.color.graph_legend_two_height,
            R.color.colorPrimary,
            R.color.azure,
            R.color.pinkishGrey
        )
        for ((index, child) in filteredData.withIndex()) {
            when (graphSelectedDisplayStyle) {
                GraphDisplayStyle.ONE_MONTH -> {
                    setMonthGraphData(child, heightColors[index], weightColors[index], index, data , targetDate)
                }
                else -> {
                    setThreeMonthGraphData(
                        child,
                        heightColors[index],
                        weightColors[index],
                        index,
                        data
                    )
                }
            }
            setLegendRecyclerData(child, heightColors[index], weightColors[index], index)
        }

        if (line_chart.data != null) {
            //Get previous dataSets
            for (dataSet in line_chart.data.dataSets) {
                data.addDataSet(dataSet)
            }
        }

        // set data
        line_chart.data = data

        val xAxis = line_chart.xAxis
        xAxis.labelCount = xAxisLabel.size
        xAxis.granularity = 1F
        xAxis.valueFormatter = BabyXAxisValueFormatter(xAxisLabel, 1)

        setYAxisLeftValue()
        setYAxisRightValue()
    }

    private fun setSwicthButtonStatus(filteredData: MutableList<ChildGrowth>) {
        if (filteredData.count() > 1) {
            btn_switch.visibility = View.VISIBLE
        } else {
            btn_switch.visibility = View.GONE
        }
    }

    private fun getXAxisValue() {
        xAxisLabel = when (graphSelectedDisplayStyle) {
            GraphDisplayStyle.ONE_MONTH -> {
                listOf(
                    "1",
                    "・",
                    "・",
                    "・",
                    "5",
                    "・",
                    "・",
                    "・",
                    "・",
                    "10",
                    "・",
                    "・",
                    "・",
                    "・",
                    "15",
                    "・",
                    "・",
                    "・",
                    "・",
                    "20",
                    "・",
                    "・",
                    "・",
                    "・",
                    "25",
                    "・",
                    "・",
                    "・",
                    "・",
                    "30",
                    "・"
                )
            }
            GraphDisplayStyle.ONE_YEAR -> listOf(
                "0月",
                "1月",
                "2月",
                "3月",
                "4月",
                "5月",
                "6月",
                "7月",
                "8月",
                "9月",
                "10月",
                "11月",
                "12月"
            )
            GraphDisplayStyle.TWENTY_FOUR_MONTHS -> listOf(
                "0ヶ月",
                "",
                "2ヶ月",
                "",
                "4ヶ月",
                "",
                "6ヶ月",
                "",
                "8ヶ月",
                "",
                "10ヶ月",
                "",
                "12ヶ月",
                "",
                "14ヶ月",
                "",
                "16ヶ月",
                "",
                "18ヶ月",
                "",
                "20ヶ月",
                "",
                "22ヶ月",
                "",
                "24ヶ月"
            )
            GraphDisplayStyle.THIRTY_SIX_MONTHS -> listOf(
                "0ヶ月",
                "",
                "",
                "3ヶ月",
                "",
                "",
                "6ヶ月",
                "",
                "",
                "9ヶ月",
                "",
                "",
                "12ヶ月",
                "",
                "",
                "15ヶ月",
                "",
                "",
                "18ヶ月",
                "",
                "",
                "21ヶ月",
                "",
                "",
                "24ヶ月",
                "",
                "",
                "27ヶ月",
                "",
                "",
                "30ヶ月",
                "",
                "",
                "33ヶ月",
                "",
                "",
                "36ヶ月"
            )
        }
    }

    private fun setWeekGraphData(
        child: ChildGrowth,
        heightColor: Int,
        weightColor: Int,
        previousIndex: Int,
        data: LineData
    ) {
        val weightValues = ArrayList<Entry>()
        val heightValues = arrayListOf<Entry>()
        for (growthData in child.growthHistories) {
            val date = Calendar.getInstance()
            date.time = growthData.measuredAt

            val dayOfWeek: Int = date.get(Calendar.DAY_OF_WEEK)
            val weekday: String = DateFormatSymbols().shortWeekdays[dayOfWeek]
            //val day = date.get(Calendar.DAY_OF_WEEK).toFloat() - 1
            var day = 7F
            xAxisLabel.forEachIndexed { i, element ->
                if (element == weekday) {
                    day = i.toFloat()
                }
            }
            weightValues.add(Entry(day, growthData.weight))
            heightValues.add(Entry(day, growthData.height))

        }
        val weightDataSet: LineDataSet
        val heightDataSet: LineDataSet
        if (line_chart.data != null && line_chart.data.dataSetCount > (previousIndex * 2)) {
            val dataIndex = previousIndex * 2
            weightDataSet = line_chart.data.getDataSetByIndex(dataIndex) as LineDataSet
            weightDataSet.values = weightValues
            heightDataSet = line_chart.data.getDataSetByIndex(dataIndex + 1) as LineDataSet
            heightDataSet.values = heightValues
            line_chart.data.notifyDataChanged()
            line_chart.notifyDataSetChanged()
        } else {
            // create a data set and give it a type
            weightDataSet = LineDataSet(weightValues, "Weight")
            weightDataSet.color = ContextCompat.getColor(requireContext(), weightColor)
            weightDataSet.setCircleColor(ContextCompat.getColor(requireContext(), weightColor))
            weightDataSet.lineWidth = 3f
            weightDataSet.circleRadius = 5f
            weightDataSet.setDrawCircleHole(false)
            weightDataSet.valueTextSize = 9f
            weightDataSet.setDrawFilled(false)

            heightDataSet = LineDataSet(heightValues, "Height")
            heightDataSet.color = ContextCompat.getColor(requireContext(), heightColor)
            heightDataSet.setCircleColor(ContextCompat.getColor(requireContext(), heightColor))
            heightDataSet.lineWidth = 3f
            heightDataSet.circleRadius = 5f
            heightDataSet.setDrawCircleHole(false)
            heightDataSet.valueTextSize = 9f
            heightDataSet.setDrawFilled(false)

            val dataSets = ArrayList<ILineDataSet>()
            dataSets.add(weightDataSet)
            dataSets.add(heightDataSet)

            data.addDataSet(weightDataSet)
            data.addDataSet(heightDataSet)
        }
        line_chart.xAxis.axisMinimum = 0F
        line_chart.xAxis.axisMaximum = 6F
    }

    private fun setMonthGraphData(
        child: ChildGrowth,
        heightColor: Int,
        weightColor: Int,
        previousIndex: Int,
        data: LineData,
        targetDate: Calendar
    ) {
        var selectedChildGender: String? = null
        var size = xAxisLabel.indices.count()
        val weightValues = ArrayList<Entry>()
        val heightValues = arrayListOf<Entry>()
        var selectedChildId = viewModel?.getSelectedChildId()
        var tempEntryHeight = Entry(0.0f, 0.0f)
        var tempEntryWeight = Entry(0.0f, 0.0f)
        for (index in 1..size) {
            var isFuture: Boolean = false
            for (growthData in child.growthHistories) {
                val day = growthData.measuredAt.toDayFormat().toInt()
                tempEntryWeight.x = index.toFloat()
                tempEntryHeight.x = index.toFloat()
                if (index == day) {
                    tempEntryHeight.y = growthData.height
                    tempEntryWeight.y = growthData.weight
                    break
                }
            }
            var dt = Date()
            val c = getInstance()
            c.time = currentDate.time
            c.add(Calendar.DATE, index - 1)
            dt = c.time
            isFuture = checkIsFutureDate(dt)
            Log.i("", "" + currentDate)
            if (tempEntryHeight.y != 0.0f && tempEntryWeight.y != 0.0f && !isFuture) {
                heightValues.add(Entry(tempEntryHeight.x, tempEntryHeight.y))
                weightValues.add(Entry(tempEntryWeight.x, tempEntryWeight.y))
            }
        }
        val weightDataSet: LineDataSet
        val heightDataSet: LineDataSet
        if (line_chart.data != null && line_chart.data.dataSetCount > (previousIndex * 2)) {
            val dataIndex = previousIndex * 2
            weightDataSet = line_chart.data.getDataSetByIndex(dataIndex) as LineDataSet
            weightDataSet.values = weightValues
            heightDataSet = line_chart.data.getDataSetByIndex(dataIndex + 1) as LineDataSet
            heightDataSet.values = heightValues
            line_chart.data.notifyDataChanged()
            line_chart.notifyDataSetChanged()
        } else {
            // create a data set and give it a type
            weightDataSet = LineDataSet(weightValues, "Weight")
            weightDataSet.color = ContextCompat.getColor(requireContext(), weightColor)
            weightDataSet.setCircleColor(ContextCompat.getColor(requireContext(), weightColor))
            weightDataSet.lineWidth = 3f
            weightDataSet.circleRadius = 5f
            weightDataSet.setDrawCircleHole(false)
            weightDataSet.valueTextSize = 0f
            weightDataSet.setDrawFilled(false)
            weightDataSet.axisDependency = YAxis.AxisDependency.RIGHT

            heightDataSet = LineDataSet(heightValues, "Height")
            heightDataSet.color = ContextCompat.getColor(requireContext(), heightColor)
            heightDataSet.setCircleColor(ContextCompat.getColor(requireContext(), heightColor))
            heightDataSet.lineWidth = 3f
            heightDataSet.circleRadius = 5f

            heightDataSet.setDrawCircleHole(false)

            heightDataSet.valueTextSize = 0f
            heightDataSet.setDrawFilled(false)
            heightDataSet.axisDependency = YAxis.AxisDependency.LEFT

            if (selectedChildId == child.childId) {
                weightDataSet.color = ContextCompat.getColor(requireContext(), weightColor)
                heightDataSet.color = ContextCompat.getColor(requireContext(), heightColor)
                weightDataSet.setCircleColor(ContextCompat.getColor(requireContext(), weightColor))
                heightDataSet.setCircleColor(ContextCompat.getColor(requireContext(), heightColor))
            } else {
                weightDataSet.color = getColorWithAlpha(
                    ContextCompat.getColor(
                        requireContext(),
                        weightColor
                    ), 0.2f
                )
                heightDataSet.color = getColorWithAlpha(
                    ContextCompat.getColor(
                        requireContext(),
                        heightColor
                    ), 0.2f
                )
                weightDataSet.setCircleColor(
                    getColorWithAlpha(
                        ContextCompat.getColor(
                            requireContext(),
                            weightColor
                        ), 0.2f
                    )
                )
                heightDataSet.setCircleColor(
                    getColorWithAlpha(
                        ContextCompat.getColor(
                            requireContext(),
                            heightColor
                        ), 0.2f
                    )
                )
            }
            if (selectedChildId == child.childId) {
                selectedChildGender = child.gender
                var monthDiff = findAgeOfSelectedChild(child.birthDay,targetDate)
                if (monthDiff <= 37) {
                    get1monthBackgroundImage(monthDiff, selectedChildGender)
                }
            }
            data.addDataSet(weightDataSet)
            data.addDataSet(heightDataSet)
        }
        line_chart.xAxis.axisMinimum = 1F
        line_chart.xAxis.axisMaximum = currentDate.getActualMaximum(Calendar.DAY_OF_MONTH).toFloat()
    }

    private fun checkIsFutureDate(dt: Date): Boolean {
        val current = Date()
        val next = dt
        return next.after(current)
    }

    private fun get1monthBackgroundImage(monthDiff: Int, selectedChildGender: String) {
        selectedChildGender.let {
            val assetManager: AssetManager? = context?.assets
            var ims: InputStream? = null
            if (selectedChildGender.equals(AppConstants.GENDER_MALE)) {
                var sb = StringBuilder()
                sb.append(AppConstants.URL_1_MONTH_BOY).append(AppConstants.URL_2_MONTH_BOY).append(
                    monthDiff
                ).append(AppConstants.IMAGE_EXTENSION)
                var imageName = sb.toString()
                ims = assetManager?.open(imageName)
            } else {
                var sb = StringBuilder()
                sb.append(AppConstants.URL_1_MONTH_GIRL).append(AppConstants.URL_2_MONTH_GIRL)
                    .append(
                        monthDiff
                    ).append(AppConstants.IMAGE_EXTENSION)
                var imageName = sb.toString()
                ims = assetManager?.open(imageName)
            }
            ims?.let {
                val d: Drawable = Drawable.createFromStream(ims, null)
                img_average.setImageDrawable(d)
            }
        }
    }

    private fun findAgeOfSelectedChild(birthDay: String, targetDate: Calendar): Int {
        var birthdayInCalender: Calendar = convertChildBirthDateToCal(birthDay)
        val diff: Long = targetDate.timeInMillis - birthdayInCalender.timeInMillis
        val calDiff = Calendar.getInstance()
        calDiff.timeInMillis = diff
        val yearDiff = calDiff.get(Calendar.YEAR) - 1970
        val monthDiff = calDiff.get(Calendar.MONTH)
        return (yearDiff * 12) + monthDiff
    }

    private fun setThreeMonthGraphData(
        child: ChildGrowth,
        heightColor: Int,
        weightColor: Int,
        previousIndex: Int,
        data: LineData
    ) {
        var selctedChildGender: String? = null
        val weightValues = ArrayList<Entry>()
        val heightValues = arrayListOf<Entry>()
        Log.i("measuredat=", "measuredat=" + child.growthHistories[0].measuredAt)
        var selectedChildId = viewModel?.getSelectedChildId()
        val averageHeightWeight = getAverageGrowthData(child)

        for ((index, growth) in averageHeightWeight.withIndex()) {
            if (growth.second.weight != 0.0f && growth.second.height != 0.0f && growth.first) {
                weightValues.add(Entry(index.toFloat(), growth.second.weight))
                heightValues.add(Entry(index.toFloat(), growth.second.height))
            }
        }
        val weightDataSet: LineDataSet
        val heightDataSet: LineDataSet
        if (line_chart.data != null && line_chart.data.dataSetCount > (previousIndex * 2)) {
            val dataIndex = previousIndex * 2
            weightDataSet = line_chart.data.getDataSetByIndex(dataIndex) as LineDataSet
            weightDataSet.values = weightValues
            heightDataSet = line_chart.data.getDataSetByIndex(dataIndex + 1) as LineDataSet
            heightDataSet.values = heightValues
            line_chart.data.notifyDataChanged()
            line_chart.notifyDataSetChanged()
        } else {
            // create a data set and give it a type
            weightDataSet = LineDataSet(weightValues, "Weight")
            weightDataSet.lineWidth = 3f
            weightDataSet.circleRadius = 5f
            weightDataSet.setDrawCircleHole(false)
            weightDataSet.valueTextSize = 0f
            weightDataSet.setDrawFilled(false)
            weightDataSet.axisDependency = YAxis.AxisDependency.RIGHT

            heightDataSet = LineDataSet(heightValues, "Height")
            heightDataSet.lineWidth = 3f
            heightDataSet.circleRadius = 5f
            heightDataSet.setDrawCircleHole(false)
            heightDataSet.valueTextSize = 0f
            heightDataSet.setDrawFilled(false)
            heightDataSet.axisDependency = YAxis.AxisDependency.LEFT
            if (selectedChildId == child.childId) {
                selctedChildGender = child.gender
                weightDataSet.color = ContextCompat.getColor(requireContext(), weightColor)
                heightDataSet.color = ContextCompat.getColor(requireContext(), heightColor)
                weightDataSet.setCircleColor(ContextCompat.getColor(requireContext(), weightColor))
                heightDataSet.setCircleColor(ContextCompat.getColor(requireContext(), heightColor))
            } else {
                weightDataSet.color = getColorWithAlpha(
                    ContextCompat.getColor(
                        requireContext(),
                        weightColor
                    ), 0.2f
                )
                heightDataSet.color = getColorWithAlpha(
                    ContextCompat.getColor(
                        requireContext(),
                        heightColor
                    ), 0.2f
                )
                weightDataSet.setCircleColor(
                    getColorWithAlpha(
                        ContextCompat.getColor(
                            requireContext(),
                            weightColor
                        ), 0.2f
                    )
                )
                heightDataSet.setCircleColor(
                    getColorWithAlpha(
                        ContextCompat.getColor(
                            requireContext(),
                            heightColor
                        ), 0.2f
                    )
                )
            }
            data.addDataSet(weightDataSet)
            data.addDataSet(heightDataSet)
        }
        when (graphSelectedDisplayStyle) {
            GraphDisplayStyle.TWENTY_FOUR_MONTHS -> {
                getAverageImage(GraphDisplayStyle.TWENTY_FOUR_MONTHS, selctedChildGender)
                line_chart.xAxis.axisMinimum = 0F
                line_chart.xAxis.axisMaximum = 24F
                line_chart.xAxis.labelRotationAngle = -45f
            }
            GraphDisplayStyle.THIRTY_SIX_MONTHS -> {
                getAverageImage(GraphDisplayStyle.THIRTY_SIX_MONTHS, selctedChildGender)
                line_chart.xAxis.axisMinimum = 0F
                line_chart.xAxis.axisMaximum = 36F
                line_chart.xAxis.labelRotationAngle = -45f
            }
            else -> {
                getAverageImage(GraphDisplayStyle.ONE_YEAR, selctedChildGender)
                line_chart.xAxis.axisMinimum = 0F
                line_chart.xAxis.axisMaximum = 12F
                line_chart.xAxis.labelRotationAngle = -45f
            }
        }
    }

    private fun getAverageImage(
        graphDisplayStyle: GraphDisplayStyle,
        selectedChildGender: String?
    ) {
        try {
            selectedChildGender?.let {
                val assetManager: AssetManager? = context?.assets
                var ims: InputStream? = null
                when (graphDisplayStyle) {
                    GraphDisplayStyle.ONE_YEAR -> {
                        if (selectedChildGender.equals(AppConstants.GENDER_MALE)) {
                            ims = assetManager?.open(AppConstants.TWELVE_MONTHS_MALE)
                        } else {
                            ims = assetManager?.open(AppConstants.TWELVE_MONTHS_FEMALE)
                        }
                    }
                    GraphDisplayStyle.TWENTY_FOUR_MONTHS -> {
                        if (selectedChildGender.equals(AppConstants.GENDER_MALE)) {
                            ims = assetManager?.open(AppConstants.TWENTY_FOUR_MONTHS_MALE)
                        } else {
                            ims = assetManager?.open(AppConstants.TWENTY_FOUR_MONTHS_FEMALE)
                        }
                    }
                    GraphDisplayStyle.THIRTY_SIX_MONTHS -> {
                        if (selectedChildGender.equals(AppConstants.GENDER_MALE)) {
                            ims = assetManager?.open(AppConstants.THIRTY_SIX_MONTHS_MALE)
                        } else {
                            ims = assetManager?.open(AppConstants.THIRTY_SIX_MONTHS_FEMALE)
                        }
                    }
                }
                ims?.let {
                    val d: Drawable = Drawable.createFromStream(ims, null)
                    img_average.setImageDrawable(d)
                }
            }
        } catch (ex: IOException) {
            return
        }
    }

    fun getColorWithAlpha(color: Int, ratio: Float): Int {
        var newColor = 0
        val alpha = Math.round(Color.alpha(color) * ratio).toInt()
        val r: Int = Color.red(color)
        val g: Int = Color.green(color)
        val b: Int = Color.blue(color)
        newColor = Color.argb(alpha, r, g, b)
        return newColor
    }

    private fun setYAxisLeftValue() {
        line_chart.axisLeft.axisMinimum = 20f
        line_chart.axisLeft.axisMaximum = 100f
        line_chart.axisLeft.isGranularityEnabled = true
        line_chart.axisLeft.granularity = 10F
        line_chart.axisLeft.setLabelCount(9, true)
        line_chart.axisLeft.isEnabled = true
        line_chart.axisLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
    }

    private fun setYAxisRightValue() {
        line_chart.axisRight.setLabelCount(9, true)
        line_chart.axisRight.axisMinimum = 0F
        line_chart.axisRight.axisMaximum = 24F
        line_chart.axisRight.isEnabled = true
        line_chart.axisRight.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
    }

    private fun filterGraphData(style: GraphDisplayStyle, isForced: Boolean = false) {
        if (!isForced && graphSelectedDisplayStyle == style) return
        graphSelectedDisplayStyle = style
        //currentDate=Calendar.getInstance()

        val targetDate = Calendar.getInstance()
        when (graphSelectedDisplayStyle) {
            GraphDisplayStyle.ONE_MONTH -> {
                currentDate.set(Calendar.HOUR_OF_DAY, 0)
                currentDate.set(Calendar.MINUTE, 0)
                currentDate.set(Calendar.SECOND, 0)
                currentDate.set(Calendar.MILLISECOND, 0)
                targetDate.time = currentDate.time
                val avgDatesArray = ArrayList<Date>()
                val arrayData = Calendar.getInstance()
                arrayData.time = currentDate.time
                //commenting code--------------------
                getXAxisValue()
                Log.i("target", "" + targetDate)
                currentDate.set(Calendar.DAY_OF_MONTH, 1)
                targetDate.set(
                    Calendar.DAY_OF_MONTH,
                    currentDate.getActualMaximum(Calendar.DAY_OF_MONTH)
                )
                //finding out the target dates that comes in the xAxis for monthly
                arrayData.add(currentDate.get(Calendar.MONTH), -0)
                avgDatesArray.add(arrayData.time)

                setGraphDisplayDateRange(targetDate, currentDate)

                val growthHistories = realm.where<GrowthHistories>()
                    .between("measuredAt", currentDate.time, targetDate.time)
                    .sort("measuredAt")
                    .findAll()
                Log.i("target value=","target value="+targetDate.time)
                for (child in childGrowthData) {
                    child.growthHistories.clear()
                }
                for (growth in growthHistories) {
                    for (child in childGrowthData) {
                        if (child.childId == growth.childId) {
                            child.growthHistories.add(growth)
                            break
                        }
                    }
                }
                legendAdapter.clearData()
                legendAdapter.notifyDataSetChanged()
                clearGraphChart()
                line_chart.extraBottomOffset = 20.0f
                setGraphData(childGrowthData,targetDate)
                //sort by descending for finding maximum child id
                //commenting code----------------------
                // childGrowthData.sortByDescending { it.childId }

                //Calculate age of child
                //replace with array of dates

                //commmenting average coloring graph-----------------
                //setStandardGraph(avgDatesArray)
                childGrowthData.sortBy { it.childId }

            }
            else -> {
                //looping into each child
                for (child in childGrowthData) {
                    child.growthHistories.clear()
                    val dob = child.birthDay
                    var month: String? = ""
                    var dd: String? = ""
                    var year: String? = ""
                    try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd")
                        val d = sdf.parse(dob)
                        val cal = Calendar.getInstance()
                        cal.time = d
                        month = checkDigit(cal[Calendar.MONTH] + 1)
                        dd = checkDigit(cal[Calendar.DATE])
                        year = checkDigit(cal[Calendar.YEAR])
                        currentDate.time = cal.time
                        Log.i("", "currentdate=" + currentDate)
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                    val targetDate = Calendar.getInstance()
                    targetDate.time = currentDate.time
                    val avgDatesArray = ArrayList<Date>()
                    val arrayData = Calendar.getInstance()
                    arrayData.time = currentDate.time
                    //commenting code--------------------
                    getXAxisValue()

                    when (style) {
                        GraphDisplayStyle.TWENTY_FOUR_MONTHS -> {
                            currentDate.set(Calendar.DAY_OF_MONTH, 1)
                            var c = Calendar.getInstance()
                            c.time = currentDate.time
                            c.add(Calendar.YEAR, 2)
                            targetDate.time = c.time
                            //finding out the target dates that comes in the xAxis for three month
                            arrayData.add(currentDate.get(Calendar.MONTH), -0)
                            val dateToAdd: Date = arrayData.time
                            avgDatesArray.add(dateToAdd)
                            for (i in 1..2) {
                                arrayData.add(Calendar.MONTH, -1)
                                avgDatesArray.add(arrayData.time)
                            }
                        }
                        GraphDisplayStyle.THIRTY_SIX_MONTHS -> {
                            currentDate.set(Calendar.DAY_OF_MONTH, 1)
                            var c = Calendar.getInstance()
                            c.time = currentDate.time
                            c.add(Calendar.YEAR, 3)
                            targetDate.time = c.time
                            arrayData.add(currentDate.get(Calendar.MONTH), -0)
                            val dateToAdd: Date = arrayData.time
                            avgDatesArray.add(dateToAdd)
                            for (i in 1..5) {
                                arrayData.add(Calendar.MONTH, -1)
                                avgDatesArray.add(arrayData.time)
                            }
                        }
                        GraphDisplayStyle.ONE_YEAR -> {
                            currentDate.set(Calendar.DAY_OF_MONTH, 1)
                            var c = Calendar.getInstance()
                            c.time = currentDate.time
                            c.add(Calendar.YEAR, 1)
                            targetDate.time = c.time

                            arrayData.add(currentDate.get(Calendar.MONTH), -0)
                            val dateToAdd: Date = arrayData.time
                            avgDatesArray.add(dateToAdd)
                            for (i in 1..11) {
                                arrayData.add(Calendar.MONTH, -1)
                                avgDatesArray.add(arrayData.time)
                            }
                        }
                    }
                    val growthHistories = realm.where<GrowthHistories>()
                        .equalTo("childId", child.childId)
                        .between("measuredAt", currentDate.time, targetDate.time)
                        .sort("measuredAt")
                        .findAll()

                    for (growth in growthHistories) {
                        if (child.childId == growth.childId) {
                            child.growthHistories.add(growth)
                        }
                    }
                }
                setGraphDisplayDateRange(targetDate, currentDate)
            }
        }
        legendAdapter.clearData()
        legendAdapter.notifyDataSetChanged()
        clearGraphChart()
        line_chart.extraBottomOffset = 20.0f
        setGraphData(childGrowthData,targetDate)
        //Calculate age of child
        //replace with array of dates

        //commmenting average coloring graph-----------------
        //setStandardGraph(avgDatesArray)
        childGrowthData.sortBy { it.childId }
    }

    fun checkDigit(number: Int): String? {
        return if (number <= 9) "0$number" else number.toString()
    }

    private fun setStandardGraph(avgDatesArray: ArrayList<Date>) {
        val ageDiff = findDateRangeForAverage(
            avgDatesArray,
            childGrowthData[0]
        )
        //find out the gender of the child which has maximum ID
        val gender = childGrowthData[childGrowthData.size - 1].gender
        val tagArray = ArrayList<String>()
        //appending strings for taking the particular data from json file
        for (i in 0 until ageDiff.size) {
            val strYearMonth = StringBuilder()
            if (ageDiff[i].first == 0) {//year count is 0
                strYearMonth.append(ageDiff[i].second).append("ヶ月")
            } else if (ageDiff[i].first >= 1) {// if more than 0
                strYearMonth.append(ageDiff[i].first).append("歳").append(ageDiff[i].second)
                    .append("ヶ月")
            }
            tagArray.add(strYearMonth.toString())
        }
        val tagArraySorted = ArrayList<String>()
        for (item in tagArray.size - 1 downTo 0) {
            tagArraySorted.add(tagArray[item])
        }
        //adding id based on GENDER to fetch data from JSON
        val id = if (gender == AppConstants.GENDER_MALE) {
            0
        } else {
            1
        }
        //fetching data from json
        getJsonData(id, tagArraySorted)
    }

    //getting json data
    private fun getJsonData(id: Int, ageGroupArray: ArrayList<String>) {
        val growthHistoryArray = ArrayList<StandardGrowthValue>()
        val data = readJsonFromAssets()
        val gson = Gson()
        //converting to POJO class
        val generalGrowthDataList = gson.fromJson(data, StandardGrowthModel::class.java)
        generalGrowthDataList?.let { standardGrowthModel ->
            val itemList = standardGrowthModel.general_growth_value
            for (item in itemList) {
                if (item.id == id) { //fetching data
                    for (ageString in ageGroupArray) {
                        // find out the Object based on the ageGroup
                        val standardValueItem: StandardGrowthValue? =
                            item.standard_growth_value.find { it.age_group == ageString }
                        standardValueItem?.let {
                            growthHistoryArray.add(standardValueItem)
                        } ?: kotlin.run {
                            //return object with 0 values when has no data
                            growthHistoryArray.add(
                                StandardGrowthValue(
                                    "", Height(0.0, 0.0, 0.0),
                                    Weight(0.0, 0.0, 0.0)
                                )
                            )
                        }

                    }

                }
            }
        }
        //setting data to lineChart
        setAvgGraphData(growthHistoryArray)
    }

    private fun setAvgGraphData(growthHistoryArray: ArrayList<StandardGrowthValue>) {
        growthHistoryArray.sortBy { it.age_group }//sorting for plotting values
        val heightMaxValues = ArrayList<Entry>()
        val heightMinValues = ArrayList<Entry>()
        val weightMaxValues = ArrayList<Entry>()
        val weightMinValues = ArrayList<Entry>()
        val heightMaxArray = ArrayList<Double>()
        val heightMinArray = ArrayList<Double>()
        val weightMaxArray = ArrayList<Double>()
        val weightMinArray = ArrayList<Double>()

        for (growthData in growthHistoryArray) {
            heightMaxArray.add((growthData.height.max))
            heightMinArray.add((growthData.height.min))
            weightMaxArray.add((growthData.weight.max))
            weightMinArray.add((growthData.weight.min))
        }
        for ((index, heightMax) in heightMaxArray.withIndex()) {
            when (graphSelectedDisplayStyle) {
                GraphDisplayStyle.ONE_MONTH -> {
                    val numberOfDays: Int = currentDate.getActualMaximum(Calendar.DAY_OF_MONTH)
                    for (i in 0 until numberOfDays + 2) {
                        heightMaxValues.add(Entry(i.toFloat(), heightMax.toFloat()))
                    }
                }
                else -> {
                    heightMaxValues.add(Entry(index.toFloat(), heightMax.toFloat()))
                }
            }
        }
        for ((index, heightMin) in heightMinArray.withIndex()) {
            when (graphSelectedDisplayStyle) {
                GraphDisplayStyle.ONE_MONTH -> {
                    val numberOfDays: Int = currentDate.getActualMaximum(Calendar.DAY_OF_MONTH)
                    for (value in 0 until numberOfDays + 2) {
                        heightMinValues.add(Entry(value.toFloat(), heightMin.toFloat()))
                    }
                }
                else -> {
                    heightMinValues.add(Entry(index.toFloat(), heightMin.toFloat()))
                }
            }
        }
        for ((index, weightMax) in weightMaxArray.withIndex()) {
            when (graphSelectedDisplayStyle) {
                GraphDisplayStyle.ONE_MONTH -> {
                    val numberOfDays: Int = currentDate.getActualMaximum(Calendar.DAY_OF_MONTH)
                    for (value in 0 until numberOfDays + 2) {
                        weightMaxValues.add(Entry(value.toFloat(), weightMax.toFloat()))
                    }
                }
                else -> {
                    weightMaxValues.add(Entry(index.toFloat(), weightMax.toFloat()))
                }
            }
        }
        for ((index, weightMin) in weightMinArray.withIndex()) {
            when (graphSelectedDisplayStyle) {
                GraphDisplayStyle.ONE_MONTH -> {
                    val numberOfDays: Int = currentDate.getActualMaximum(Calendar.DAY_OF_MONTH)
                    for (value in 0 until numberOfDays + 2) {
                        weightMinValues.add(Entry(value.toFloat(), weightMin.toFloat()))
                    }
                }
                else -> {
                    weightMinValues.add(Entry(index.toFloat(), weightMin.toFloat()))
                }
            }
        }

        val avgHeightMaxDataSet: LineDataSet
        val avgHeightMinDataSet: LineDataSet
        val avgWeightMaxDataSet: LineDataSet
        val avgWeightMinDataSet: LineDataSet

        if (line_chart.data != null && line_chart.data.dataSetCount > 0) {

            avgHeightMinDataSet = LineDataSet(heightMinValues, "AverageHeightMin")

            avgHeightMaxDataSet = LineDataSet(heightMaxValues, "AverageHeightMax")

            avgWeightMaxDataSet = LineDataSet(weightMaxValues, "AverageWeightMax")

            avgWeightMinDataSet = LineDataSet(weightMinValues, "AverageWeightMin")
            //setting data to graph
            avgHeightMinDataSet.color = ContextCompat.getColor(
                requireContext(),
                R.color.avgHeightColor
            )
            avgHeightMaxDataSet.color = ContextCompat.getColor(
                requireContext(),
                R.color.avgHeightColor
            )
            avgWeightMinDataSet.color = ContextCompat.getColor(
                requireContext(),
                R.color.avgWeightColor
            )
            avgWeightMaxDataSet.color = ContextCompat.getColor(
                requireContext(),
                R.color.avgWeightColor
            )
            avgWeightMinDataSet.setDrawFilled(true)
            avgHeightMinDataSet.setDrawFilled(true)
            avgHeightMaxDataSet.setDrawCircles(false)
            avgHeightMinDataSet.setDrawCircles(false)
            avgWeightMaxDataSet.setDrawCircles(false)
            avgWeightMinDataSet.setDrawCircles(false)
            avgWeightMinDataSet.lineWidth = 3f
            avgWeightMaxDataSet.lineWidth = 3f
            avgHeightMinDataSet.lineWidth = 3f
            avgHeightMaxDataSet.lineWidth = 3f
            avgWeightMinDataSet.valueTextSize = 9f
            avgWeightMaxDataSet.valueTextSize = 9f
            avgHeightMinDataSet.valueTextSize = 9f
            avgHeightMaxDataSet.valueTextSize = 9f

            avgHeightMaxDataSet.axisDependency = YAxis.AxisDependency.LEFT
            avgHeightMinDataSet.axisDependency = YAxis.AxisDependency.LEFT
            avgWeightMaxDataSet.axisDependency = YAxis.AxisDependency.RIGHT
            avgWeightMinDataSet.axisDependency = YAxis.AxisDependency.RIGHT

            //getting values
            avgHeightMinDataSet.fillFormatter = MyFillFormatter(avgHeightMaxDataSet)
            avgWeightMinDataSet.fillFormatter = MyFillFormatter(avgWeightMaxDataSet)

            //adding color between 2 lines
            line_chart.renderer = MyLineLegendRenderer(
                line_chart,
                line_chart.animator,
                line_chart.viewPortHandler,
                context
            )

            data.addDataSet(avgWeightMinDataSet)
            data.addDataSet(avgWeightMaxDataSet)
            data.addDataSet(avgHeightMinDataSet)
            data.addDataSet(avgHeightMaxDataSet)
            line_chart.data.setDrawValues(false)
            line_chart.notifyDataSetChanged()
        }
    }

    //getting json data
    private fun readJsonFromAssets(): String? {
        val json: String?
        try {
            //Standard child growth data is fetching from standard_child_growth.json
            val inputStream: InputStream =
                context?.assets?.open("standard_child_growth.json") ?: return null
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val charset: Charset = Charsets.UTF_8
            json = String(buffer, charset)
            return json
        } catch (e: IOException) {
            debugLogInfo("Exception: $e")
        }
        return null
    }

    //For Calculate the age difference
    private fun findDateRangeForAverage(
        childGrowthDates: ArrayList<Date>,
        childGrowth: ChildGrowth
    ): ArrayList<Pair<Int, Int>> {
        val ageArray = ArrayList<Pair<Int, Int>>()
        val birthDate: Calendar = convertChildBirthDateToCal(childGrowth.birthDay)
        for (i in 0 until childGrowthDates.size) {
            val diff: Long = childGrowthDates[i].time - birthDate.timeInMillis
            val calDiff = Calendar.getInstance()
            calDiff.timeInMillis = diff
            val yearDiff = calDiff.get(Calendar.YEAR) - 1970
            val monthDiff = calDiff.get(Calendar.MONTH)
            ageArray.add(Pair(yearDiff, monthDiff))
        }

        return ageArray
    }

    private fun convertChildBirthDateToCal(birthDay: String): Calendar {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN)
        val date: Date = sdf.parse(birthDay) ?: Date()
        val cal: Calendar = Calendar.getInstance()
        cal.time = date
        return cal
    }

    private fun clearGraphChart() {
        line_chart.fitScreen()
        data.clearValues()
        line_chart.data = null
        line_chart.xAxis.valueFormatter = null
        line_chart.clear()
        line_chart.invalidate()
    }

    private fun showGraphDisplayTypeSelector() {
        val data = arrayOf(
            String.format(getString(R.string.n_month), 1),
            String.format(getString(R.string.n_month), 12),
            String.format(getString(R.string.n_month), 24),
            String.format(getString(R.string.n_month), 36)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setItems(data) { dialog, which ->
                dialog.dismiss()
                date_range_type.text = data[which]
                backCount = 0
                viewModel?.setBirthday(childGrowthData[index])
                viewModel?.setSelectedChildId(childGrowthData[index])
                when (which) {
                    0 -> {
                        if (graphSelectedDisplayStyle != GraphDisplayStyle.ONE_MONTH) {
                            currentDate = Calendar.getInstance()
                            date_previous_button.isEnabled = true
                            date_next_button.isEnabled = false
                            date_previous_button.setTintColor(requireContext(), R.color.red)
                            date_next_button.setTintColor(requireContext(), R.color.pinkishGrey)
                        }
                        filterGraphData(GraphDisplayStyle.ONE_MONTH)
                    }
                    1 -> {
                        if (graphSelectedDisplayStyle != GraphDisplayStyle.ONE_YEAR) {
                            currentDate = Calendar.getInstance()
                            setPrevNextButtonStatus()
                        }
                        filterGraphData(GraphDisplayStyle.ONE_YEAR)
                    }
                    2 -> {
                        if (graphSelectedDisplayStyle != GraphDisplayStyle.TWENTY_FOUR_MONTHS) {
                            currentDate = Calendar.getInstance()
                            setPrevNextButtonStatus()
                        }
                        filterGraphData(GraphDisplayStyle.TWENTY_FOUR_MONTHS)
                    }
                    3 -> {
                        if (graphSelectedDisplayStyle != GraphDisplayStyle.THIRTY_SIX_MONTHS) {
                            currentDate = Calendar.getInstance()
                            setPrevNextButtonStatus()
                        }
                        filterGraphData(GraphDisplayStyle.THIRTY_SIX_MONTHS)
                    }
                }
            }
            .show()
    }

    private fun setPrevNextButtonStatus() {
        date_previous_button.isEnabled = false
        date_next_button.isEnabled = false
        date_previous_button.setTintColor(requireContext(), R.color.pinkishGrey)
        date_next_button.setTintColor(requireContext(), R.color.pinkishGrey)
    }

    @SuppressLint("SetTextI18n")
    private fun setGraphDisplayDateRange(targetDate: Calendar, currentDate: Calendar) {
        when (graphSelectedDisplayStyle) {
            GraphDisplayStyle.ONE_MONTH -> {
                date_range.text =
                    "${currentDate.time.toMonthDay()}の平均 - ${targetDate.time.toDotDateFormatYMD()}"
            }
            GraphDisplayStyle.ONE_YEAR -> {
                date_range.text = AppConstants.TWELVE_MONTHS
            }
            GraphDisplayStyle.TWENTY_FOUR_MONTHS -> {
                date_range.text = AppConstants.TWENTY_FOUR_MONTHS
            }
            GraphDisplayStyle.THIRTY_SIX_MONTHS -> {
                date_range.text = AppConstants.THIRTY_SIX_MONTHS

            }
        }
    }

    private fun handleDateRangeChangeButtonClick(isDecrease: Boolean) {
        when (graphSelectedDisplayStyle) {
            GraphDisplayStyle.ONE_MONTH -> {
                if (isDecrease) {
                    handleDateDecrease(Calendar.MONTH, -1)
                } else {
                    handleDateIncrease(Calendar.MONTH, 1)
                }
            }
            GraphDisplayStyle.ONE_YEAR -> {
                if (isDecrease) {
                    handleDateDecrease(Calendar.YEAR, -1)
                } else {
                    handleDateIncrease(Calendar.YEAR, 1)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getDaysBetween(startDate: Calendar, endDate: Calendar): Int {
        return ChronoUnit.DAYS.between(startDate.toInstant(), endDate.toInstant()).toInt()
    }

    private fun getDaysBetweenForOldApi(startDate: Calendar, endDate: Calendar): Int {
        val start = Calendar.getInstance()
        val end = Calendar.getInstance()
        start.timeInMillis = startDate.timeInMillis
        end.timeInMillis = endDate.timeInMillis

        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)

        end.set(Calendar.HOUR_OF_DAY, 0)
        end.set(Calendar.MINUTE, 0)
        end.set(Calendar.SECOND, 0)
        end.set(Calendar.MILLISECOND, 0)

        return TimeUnit.MILLISECONDS.toDays(abs(end.timeInMillis - start.timeInMillis)).toInt()
    }

    private fun getMonthsBetweenForOldApi(startDate: Calendar, endDate: Calendar): Int {
        val start = Calendar.getInstance()
        val end = Calendar.getInstance()
        start.timeInMillis = startDate.timeInMillis
        end.timeInMillis = endDate.timeInMillis

        var monthsBetween = 0
        var dateDiff = end.get(Calendar.DAY_OF_MONTH) - start.get(Calendar.DAY_OF_MONTH)
        if (dateDiff < 0) {
            val borrow = end.getActualMaximum(Calendar.DAY_OF_MONTH)
            dateDiff = (end.get(Calendar.DAY_OF_MONTH) + borrow) - start.get(Calendar.DAY_OF_MONTH)
            monthsBetween--
            if (dateDiff > 0) {
                monthsBetween++
            }
        } else {
            monthsBetween++
        }

        monthsBetween += end.get(Calendar.MONTH) - start.get(Calendar.MONTH)
        monthsBetween += ((end[Calendar.YEAR] - start[Calendar.YEAR]) * 12)

        return monthsBetween
    }

    private fun getYearsBetweenForOldApi(startDate: Calendar, endDate: Calendar): Int {
        val start = Calendar.getInstance()
        val end = Calendar.getInstance()
        start.timeInMillis = startDate.timeInMillis
        end.timeInMillis = endDate.timeInMillis

        var diff = end.get(Calendar.YEAR) - start.get(Calendar.YEAR)
        if (start.get(Calendar.MONTH) > end.get(Calendar.MONTH) ||
            (start.get(Calendar.MONTH) == end.get(Calendar.MONTH) && start.get(Calendar.DATE) > end.get(
                Calendar.DATE
            ))
        ) {
            diff--
        }

        return diff
    }

    private fun handleDateIncrease(calenderUnit: Int, amount: Int) {
        backCount--
        val today = Calendar.getInstance()
        val targetDay = Calendar.getInstance()
        targetDay.timeInMillis = currentDate.timeInMillis
        targetDay.add(calenderUnit, amount)
        val difference = when (calenderUnit) {
            Calendar.DAY_OF_WEEK -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    getDaysBetween(targetDay, today)
                } else {
                    getDaysBetweenForOldApi(targetDay, today)
                }
            }
            Calendar.MONTH -> getMonthsBetweenForOldApi(targetDay, today)
            Calendar.YEAR -> getYearsBetweenForOldApi(targetDay, today)
            else -> getDaysBetweenForOldApi(targetDay, today)
        }
        when {
            difference >= amount -> {
                currentDate.add(calenderUnit, amount)
                filterGraphData(graphSelectedDisplayStyle, true)
            }
            targetDay.before(today) -> {
                currentDate.timeInMillis = today.timeInMillis
                filterGraphData(graphSelectedDisplayStyle, true)
                date_next_button.isEnabled = false
                date_next_button.setTintColor(requireContext(), R.color.pinkishGrey)
            }
            else -> {
                date_next_button.isEnabled = false
                date_next_button.setTintColor(requireContext(), R.color.pinkishGrey)
            }
        }
    }

    private fun handleDateDecrease(calenderUnit: Int, amount: Int) {
        backCount++
        currentDate.add(calenderUnit, amount)
        date_next_button.isEnabled = true
        date_next_button.setTintColor(requireContext(), R.color.red)
        filterGraphData(graphSelectedDisplayStyle, true)
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

    private fun setTodayWordVisibility(isVisible: Boolean) {
        if (isVisible) {
            quote_label.visibility = View.VISIBLE
            today_word.visibility = View.VISIBLE
            today_word_divider.visibility = View.VISIBLE
        } else {
            quote_label.visibility = View.GONE
            today_word.visibility = View.GONE
            today_word_divider.visibility = View.GONE
        }
    }

    private fun setTodayWord(children: List<ChildrenModel>) {
        if (children.isEmpty()) return

        val utils = DateUtils()
        val currentDate = Calendar.getInstance()
        val birthCalender = Calendar.getInstance()
        val todayWord = getDayWord(currentDate)
        val child = children.last()

        val dateFormat = if (child.birthDay.contains("/")) {
            SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN)
        }

        birthCalender.time = dateFormat.parse(child.birthDay) ?: currentDate.time

        val monthDiff = utils.getDifferenceInMonths(birthCalender, currentDate)
        birthCalender.add(Calendar.MONTH, -1)
        val dayDiff = utils.getDayDifference(currentDate, birthCalender)
        val finalWord = when (child.gender) {
            "male" -> {
                getMaleSeasonalText(monthDiff, dayDiff, todayWord)
            }
            "female" -> {
                getFemaleSeasonalText(monthDiff, dayDiff, todayWord)
            }
            else -> {
                todayWord
            }
        }

        if (finalWord.isNullOrEmpty()) {
            setTodayWordVisibility(false)
        } else {
            today_word.text = finalWord
            setTodayWordVisibility(true)
        }
    }

    private fun getDayWord(currentDate: Calendar): String? {
        val jsonString = readJsonFromAsset("today_words.json")
        val todayWord = Gson().fromJson(jsonString, TodayWord::class.java)
        val monthData = todayWord.month[currentDate.get(Calendar.MONTH)]
        val day = currentDate.get(Calendar.DAY_OF_MONTH)

        monthData.info.firstOrNull { it.date == day }?.let { info ->
            return info.text
        } ?: kotlin.run {
            return null
        }
    }

    private fun getMaleSeasonalText(monthDiff: Int, day: Int, todayWord: String?): String? {
        val growthTextJson = readJsonFromAsset("growth_text_male.json")
        val growthData = Gson().fromJson(growthTextJson, GrowthText::class.java)
        val maleData = if (monthDiff < growthData.age.size) {
            growthData.age[monthDiff]
        } else {
            return todayWord
        }
        maleData.info.firstOrNull { it.date == day }?.let { info ->
            if (todayWord == null) {
                return info.text
            }
            return "$todayWord\n${info.text}"
        } ?: kotlin.run {
            return todayWord
        }
    }

    private fun getFemaleSeasonalText(monthDiff: Int, day: Int, todayWord: String?): String? {
        val growthTextJson = readJsonFromAsset("growth_text_female.json")
        val growthData = Gson().fromJson(growthTextJson, GrowthText::class.java)
        val femaleData = if (monthDiff < growthData.age.size) {
            growthData.age[monthDiff]
        } else {
            return todayWord
        }
        femaleData.info.firstOrNull { it.date == day }?.let { info ->
            if (todayWord == null) {
                return info.text
            }
            return "$todayWord\n${info.text}"
        } ?: kotlin.run {
            return todayWord
        }
    }

    private fun readJsonFromAsset(fileName: String): String? {
        val jsonString: String
        try {
            jsonString =
                requireContext().assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            debugLogInfo("Exception: $ioException")
            return null
        }
        return jsonString
    }

    private fun setLegendRecyclerView() {
        growth_info_recycler_view.layoutManager = LinearLayoutManager(requireContext())
        legendAdapter = LegendAdapter(requireContext())
        growth_info_recycler_view.adapter = legendAdapter
    }

    private fun setLegendRecyclerData(
        child: ChildGrowth,
        heightColor: Int,
        weightColor: Int,
        index: Int
    ) {
        legendAdapter.addData(child, heightColor, weightColor)
        legendAdapter.notifyItemInserted(index)
    }

    private fun setTitle() {
        val date = Date()
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
        dateFormat.format(date).toMonthDateDayFormat()?.let {
            (activity as? MainActivity)?.updateToolbarTitle("今日$it")
        }
        last_measured_date.text = "${date.toDateFormatWithDay()}"
    }

    private fun transitToArticleDetails(article: Article) {
        val detailFragment = ArticleDetailFragment()
        detailFragment.article = article
        (parentFragment as? HomeFragment)?.showFragment(detailFragment)
    }

    private fun getAverageGrowthData(childGrowth: ChildGrowth): List<Pair<Boolean, HeightWeightModel>> {
        //val averageGrowthData: MutableList<Pair<Date,HeightWeightModel>> = mutableListOf()
        val averageGrowthData: MutableList<Pair<Boolean, HeightWeightModel>> = mutableListOf()
        val target = Calendar.getInstance()
        val current = Calendar.getInstance()
        var size = 0
        when (graphSelectedDisplayStyle) {
            GraphDisplayStyle.ONE_MONTH -> {
                size = xAxisLabel.indices.count()
            }
            GraphDisplayStyle.ONE_YEAR -> {
                size = 12
            }
            GraphDisplayStyle.TWENTY_FOUR_MONTHS -> {
                size = 24
            }
            GraphDisplayStyle.THIRTY_SIX_MONTHS -> {
                size = 36
            }
        }
        Log.i("current=", "current=" + currentDate.time)
        var tempHeightWeightModel = HeightWeightModel(0.0f, 0.0f)
        val currentDate = getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        val d = sdf.parse(childGrowth.birthDay)
        currentDate.time = d// all done

        for (index in 0..size) {
            val lastMonthGrowth = childGrowth.growthHistories.filter {
                current.time = it.measuredAt
                target.time = currentDate.time
                target.add(Calendar.MONTH, +index)
                current.isSameMonth(target)
            }
            var isNotFuture: Boolean = false
            isNotFuture = !target.after(getInstance())
            Log.i("target=", "after target=" + target.time)
            if (lastMonthGrowth.isNotEmpty()) {
                val growthModel = HeightWeightModel(0F, 0F)
                for (growth in lastMonthGrowth) {
                    growthModel.height += growth.height
                    growthModel.weight += growth.weight
                }
                val averageHeightForLastMonth = growthModel.height / lastMonthGrowth.size.toFloat()
                val averageWeightForLastMonth = growthModel.weight / lastMonthGrowth.size.toFloat()
                //averageGrowthData.add(Pair(lastMonthGrowth[0].measuredAt,HeightWeightModel(averageHeightForLastMonth, averageWeightForLastMonth)))
                averageGrowthData.add(
                    Pair(
                        isNotFuture,
                        HeightWeightModel(
                            averageHeightForLastMonth,
                            averageWeightForLastMonth
                        )
                    )
                )
                tempHeightWeightModel.height = averageHeightForLastMonth
                tempHeightWeightModel.weight = averageWeightForLastMonth
            } else {
                //averageGrowthData.add(HeightWeightModel(0.0f, 0.0f))
                //averageGrowthData.add(Pair(lastMonthGrowth[0].measuredAt,HeightWeightModel(tempHeightWeightModel.height, tempHeightWeightModel.weight)))
                averageGrowthData.add(
                    Pair(
                        isNotFuture,
                        HeightWeightModel(
                            tempHeightWeightModel.height,
                            tempHeightWeightModel.weight
                        )
                    )
                )
            }
        }
        return averageGrowthData
    }

    override fun onDestroyView() {
        if (loadingDialog != null) {
            loadingDialog?.dismiss()
            loadingDialog = null
        }
        realm.close()
        super.onDestroyView()
    }

    enum class GraphDisplayStyle {
        ONE_MONTH,
        ONE_YEAR,
        TWENTY_FOUR_MONTHS,
        THIRTY_SIX_MONTHS
    }
}