package com.golfrecorder.ui.course

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.golfrecorder.data.local.entity.CourseEntity
import com.golfrecorder.data.local.entity.CourseYoutubeLinkEntity
import com.golfrecorder.data.repository.CourseRepository
import com.golfrecorder.data.repository.CourseYoutubeLinkRepository
import com.golfrecorder.ui.common.GolfCard
import com.golfrecorder.ui.common.MoreBelowIndicator
import com.golfrecorder.ui.common.PrimaryCtaButton
import com.golfrecorder.ui.common.dragElevation
import com.golfrecorder.ui.common.dragHandle
import com.golfrecorder.ui.common.rememberDragDropListState
import com.golfrecorder.ui.theme.GolfTokens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CourseManageViewModel(
    private val courseRepository: CourseRepository,
    private val courseYoutubeLinkRepository: CourseYoutubeLinkRepository,
) : ViewModel() {
    val courses: StateFlow<List<CourseEntity>> = courseRepository.getCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun reorder(orderedCourses: List<CourseEntity>) {
        viewModelScope.launch { courseRepository.reorder(orderedCourses) }
    }

    fun getYoutubeLinks(courseId: Long): Flow<List<CourseYoutubeLinkEntity>> =
        courseYoutubeLinkRepository.getLinks(courseId)
}

class CourseManageViewModelFactory(
    private val courseRepository: CourseRepository,
    private val courseYoutubeLinkRepository: CourseYoutubeLinkRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CourseManageViewModel(courseRepository, courseYoutubeLinkRepository) as T
}

@Composable
private fun ReviewSection(label: String, content: String) {
    Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    Text(content, style = MaterialTheme.typography.bodySmall)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseManageScreen(
    viewModel: CourseManageViewModel,
    onAddCourse: () -> Unit,
    onEditCourse: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    var expandedCourseIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val context = LocalContext.current

    var displayCourses by remember { mutableStateOf(courses) }
    val listState = rememberLazyListState()
    val dragState = rememberDragDropListState(
        listState = listState,
        itemCount = { displayCourses.size },
        onMove = { from, to ->
            displayCourses = displayCourses.toMutableList().apply { add(to, removeAt(from)) }
        },
        onReorderFinished = { viewModel.reorder(displayCourses) },
    )
    LaunchedEffect(courses) {
        if (dragState.draggingItemIndex == null) displayCourses = courses
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("코스 관리", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    Text(
                        "${displayCourses.size}개 코스",
                        style = MaterialTheme.typography.bodySmall,
                        color = GolfTokens.TextSecondary,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                },
            )
        },
    ) { padding ->
        if (displayCourses.isEmpty()) {
            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                Text("등록된 코스가 없습니다. 아래 버튼을 눌러 추가하세요.", color = GolfTokens.TextPrimary)
                Spacer(Modifier.height(16.dp))
                PrimaryCtaButton(
                    text = "새 코스 추가",
                    onClick = onAddCourse,
                    modifier = Modifier.height(GolfTokens.PrimaryButtonHeight),
                )
            }
            return@Scaffold
        }
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(GolfTokens.Background)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    "코스를 누르면 상세 리뷰가 펼쳐져요. ≡를 길게 눌러 순서를 바꿀 수 있어요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GolfTokens.TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = GolfTokens.PrimaryButtonHeight + 32.dp,
                        ),
                    ) {
                        itemsIndexed(displayCourses, key = { _, course -> course.id }) { index, course ->
                            val expanded = course.id in expandedCourseIds
                            GolfCard(
                                modifier = Modifier.dragElevation(index, dragState).padding(bottom = 10.dp),
                                onClick = {
                                    expandedCourseIds = if (expanded) {
                                        expandedCourseIds - course.id
                                    } else {
                                        expandedCourseIds + course.id
                                    }
                                },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "≡",
                                            modifier = Modifier.dragHandle(index, dragState).padding(end = 12.dp),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = GolfTokens.TextSecondary,
                                        )
                                        Column {
                                            Text(course.name, fontWeight = FontWeight.Bold, color = GolfTokens.TextPrimary)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (course.rating != null) {
                                                    Text(
                                                        "★ ${"%.1f".format(course.rating)}  ",
                                                        color = GolfTokens.FieldGreen,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodySmall,
                                                    )
                                                }
                                                if (course.difficulty != null) {
                                                    Text(
                                                        "난이도 ${course.difficulty}",
                                                        color = GolfTokens.ObText,
                                                        fontWeight = FontWeight.SemiBold,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        modifier = Modifier
                                                            .background(GolfTokens.ObBackground, RoundedCornerShape(GolfTokens.ChipCorner))
                                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                                    )
                                                }
                                            }
                                            val infoLine = listOfNotNull(
                                                course.region?.takeIf { it.isNotBlank() },
                                                course.distance?.takeIf { it.isNotBlank() },
                                                course.travelTime?.takeIf { it.isNotBlank() },
                                            ).joinToString(" | ")
                                            if (infoLine.isNotBlank()) {
                                                Text(infoLine, style = MaterialTheme.typography.bodySmall, color = GolfTokens.TextSecondary)
                                            }
                                            if (!course.oneLineReview.isNullOrBlank()) {
                                                Text(
                                                    course.oneLineReview.chunked(30).joinToString("\n"),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = GolfTokens.TextSecondary,
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        "✏️",
                                        modifier = Modifier
                                            .clickable { onEditCourse(course.id) }
                                            .padding(4.dp),
                                    )
                                }
                                if (expanded) {
                                    Column(modifier = Modifier.padding(top = 12.dp)) {
                                        val hasDetail = !course.transportInfo.isNullOrBlank() ||
                                            !course.clubhouseInfo.isNullOrBlank() ||
                                            !course.courseInfo.isNullOrBlank()
                                        if (!hasDetail) {
                                            Text(
                                                "입력된 리뷰 상세 정보가 없습니다. \"✏️\"에서 추가할 수 있습니다.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = GolfTokens.TextSecondary,
                                            )
                                        } else {
                                            course.transportInfo?.takeIf { it.isNotBlank() }?.let {
                                                ReviewSection("교통", it)
                                                Spacer(Modifier.height(8.dp))
                                            }
                                            course.clubhouseInfo?.takeIf { it.isNotBlank() }?.let {
                                                ReviewSection("클럽하우스", it)
                                                Spacer(Modifier.height(8.dp))
                                            }
                                            course.courseInfo?.takeIf { it.isNotBlank() }?.let {
                                                ReviewSection("코스", it)
                                            }
                                        }

                                        val youtubeLinksFlow = remember(course.id) { viewModel.getYoutubeLinks(course.id) }
                                        val youtubeLinks by youtubeLinksFlow.collectAsStateWithLifecycle(initialValue = emptyList())
                                        if (youtubeLinks.isNotEmpty()) {
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                "유튜브 링크",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = GolfTokens.FieldGreen,
                                            )
                                            youtubeLinks.forEach { link ->
                                                Text(
                                                    "▶ ${link.title ?: link.url}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = GolfTokens.FieldGreen,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.fillMaxWidth()
                                                        .clickable {
                                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url)))
                                                        }
                                                        .padding(vertical = 4.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    MoreBelowIndicator(
                        listState = listState,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
                    )
                }
            }
            PrimaryCtaButton(
                text = "새 코스 추가",
                onClick = onAddCourse,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .height(GolfTokens.PrimaryButtonHeight),
            )
        }
    }
}
