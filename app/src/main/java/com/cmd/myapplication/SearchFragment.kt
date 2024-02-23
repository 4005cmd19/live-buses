package com.cmd.myapplication

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.cmd.myapplication.data.viewModels.SearchViewModel
import com.cmd.myapplication.utils.SearchListAdapter

/**
 * A simple [Fragment] subclass.
 * Use the [SearchFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SearchFragment : Fragment(R.layout.fragment_search) {
    private val searchViewModel: SearchViewModel by activityViewModels { SearchViewModel.Factory }

    private lateinit var searchList: RecyclerView
    private val searchListAdapter = SearchListAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        searchList = view.findViewById(R.id.search_list)
        searchList.adapter = searchListAdapter

        searchViewModel.searchResults.observe(viewLifecycleOwner) {
            searchListAdapter.searchResults.apply {
                clear()
                addAll(it)
            }

            searchListAdapter.notifyDataSetChanged()
        }
    }
}