package com.par9uet.jm.ui.screens

import androidx.compose.foundation.layout.heightIn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import com.par9uet.jm.ui.components.WatchLazyColumn as LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.par9uet.jm.ui.components.watchVerticalScroll as verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Psychology
import com.par9uet.jm.ui.components.WatchAlertDialog as AlertDialog
import com.par9uet.jm.ui.components.WatchBasicAlertDialog as BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.AiPersona
import com.par9uet.jm.store.ToastManager
import com.par9uet.jm.ui.components.CommonScaffold
import com.par9uet.jm.ui.viewModel.PersonaViewModel
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaManagerScreen(
    personaViewModel: PersonaViewModel = koinViewModel(),
    toastManager: ToastManager = getKoin().get(),
) {
    val uiState by personaViewModel.uiState.collectAsState()
    var editingPersona by remember { mutableStateOf<AiPersona?>(null) }
    var deletingPersona by remember { mutableStateOf<AiPersona?>(null) }

    CommonScaffold(title = "人格面具") {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                InfoBanner(
                    text = "人格面具允许你自定义 AI 的名字、职业、年龄、简介与输出格式。所有字段均可选，留空则不注入对应设定。"
                )
            }
            item {
                NewPersonaCard(onClick = {
                    editingPersona = personaViewModel.createDraft()
                })
            }
            if (uiState.personas.isEmpty()) {
                item {
                    EmptyPersonaHint()
                }
            } else {
                items(uiState.personas, key = { it.id }) { persona ->
                    PersonaCard(
                        persona = persona,
                        isActive = persona.id == uiState.activePersonaId,
                        onSetActive = { personaViewModel.setActive(persona.id) },
                        onEdit = { editingPersona = persona },
                        onDelete = { deletingPersona = persona }
                    )
                }
            }
        }
    }

    editingPersona?.let { persona ->
        PersonaEditDialog(
            persona = persona,
            onDismiss = { editingPersona = null },
            onSave = { updated ->
                val saved = personaViewModel.save(updated)
                if (saved != null) {
                    toastManager.showAsync("已保存人格面具")
                } else {
                    toastManager.showAsync("人格面具内容为空，未保存")
                }
                editingPersona = null
            }
        )
    }

    deletingPersona?.let { persona ->
        AlertDialog(
            onDismissRequest = { deletingPersona = null },
            title = { Text("删除人格面具") },
            text = { Text("确定要删除「${persona.name.ifBlank { "未命名" }}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    personaViewModel.delete(persona.id)
                    toastManager.showAsync("已删除人格面具")
                    deletingPersona = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingPersona = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun InfoBanner(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Rounded.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun NewPersonaCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(22.dp))
                }
            }
            Text(
                "新建人格面具",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun EmptyPersonaHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.Face,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "还没有人格面具，点击上方新建",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PersonaCard(
    persona: AiPersona,
    isActive: Boolean,
    onSetActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${persona.name.ifBlank { "未命名" }}${if (isActive) " · 使用中" else ""}", style = MaterialTheme.typography.titleSmall)
            if (persona.profession.isNotBlank()) Text(persona.profession, style = MaterialTheme.typography.bodySmall)
            if (persona.bio.isNotBlank()) Text(persona.bio, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (!isActive) FilledTonalButton(onClick = onSetActive, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("设为使用") }
            TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("编辑人设") }
            TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("删除人设") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonaEditDialog(
    persona: AiPersona,
    onDismiss: () -> Unit,
    onSave: (AiPersona) -> Unit,
) {
    var name by remember { mutableStateOf(persona.name) }
    var profession by remember { mutableStateOf(persona.profession) }
    var age by remember { mutableStateOf(persona.age) }
    var bio by remember { mutableStateOf(persona.bio) }
    var outputFormat by remember { mutableStateOf(persona.outputFormat) }
    var systemPromptExtra by remember { mutableStateOf(persona.systemPromptExtra) }
    val keyboard = LocalSoftwareKeyboardController.current

    AlertDialog(onDismissRequest = onDismiss, title = { Text("编辑人格面具") }, text = {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("名字（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(profession, { profession = it }, label = { Text("身份（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(age, { age = it }, label = { Text("年龄（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(bio, { bio = it }, label = { Text("简介（可选）") }, maxLines = 4, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(outputFormat, { outputFormat = it }, label = { Text("输出格式（可选）") }, maxLines = 4, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(systemPromptExtra, { systemPromptExtra = it }, label = { Text("提示词（可选）") }, maxLines = 6, modifier = Modifier.fillMaxWidth())
        }
    }, confirmButton = { FilledTonalButton(onClick = { keyboard?.hide(); onSave(persona.copy(name = name.trim(), profession = profession.trim(), age = age.trim(), bio = bio.trim(), outputFormat = outputFormat.trim(), systemPromptExtra = systemPromptExtra.trim())) },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("取消") } })
}
