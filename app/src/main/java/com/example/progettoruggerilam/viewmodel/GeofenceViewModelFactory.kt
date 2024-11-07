import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.progettoruggerilam.repository.GeofenceRepository
import com.example.progettoruggerilam.viewmodel.GeofenceViewModel

class GeofenceViewModelFactory(
    private val application: Application,
    private val geofenceRepository: GeofenceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GeofenceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GeofenceViewModel(application, geofenceRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
