package com.example.ingredio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ingredio.databinding.MenuCupboardBinding

class menuCupboard : Fragment() {

    private var _binding: MenuCupboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CupboardViewModel by viewModels()
    private lateinit var ingredientAdapter: IngredientAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MenuCupboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }

        binding.buttonThird.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_addItemsFragment)
        }

        viewModel.fetchUserIngredients()
    }

    private fun setupRecyclerView() {
        ingredientAdapter = IngredientAdapter(emptyList(), "Remove") { ingredient ->
            viewModel.removeIngredientFromCupboard(ingredient.id)
        }
        binding.recyclerViewCupboard.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ingredientAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.userIngredients.observe(viewLifecycleOwner) { ingredients ->
            ingredientAdapter.updateIngredients(ingredients)
            binding.textviewSecond.visibility = if (ingredients.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.status.observe(viewLifecycleOwner) { status ->
            Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}