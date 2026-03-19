package app.violet.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import app.violet.R
import app.violet.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardVertimerge.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_vertimerge)
        }
        binding.cardZipmaker.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_zipmaker)
        }
        binding.cardWatermark.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_watermark)
        }
        binding.cardFormatter.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_formatter)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
