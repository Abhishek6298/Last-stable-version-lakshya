import sys

p = 'app/src/main/java/com/example/ui/screens/ChatScreen.kt'
with open(p, 'r', encoding='utf-8') as f:
    content = f.read()

old_text = '''                                            } else if (msg.text.isNotBlank()) {
                                                Text(
                                                    msg.text,
                                                    color = if (isUser) Color.White else textColor,
                                                    fontSize = 14.sp,
                                                    lineHeight = 18.sp
                                                )
                                            }'''

new_text = '''                                            } else if (msg.text.isNotBlank()) {
                                                val formattedMathText = remember(msg.text) {
                                                    com.example.util.MathFormatter.formatMathAndLatex(msg.text)
                                                }
                                                Text(
                                                    formattedMathText,
                                                    color = if (isUser) Color.White else textColor,
                                                    fontSize = 14.sp,
                                                    lineHeight = 20.sp
                                                )
                                            }'''

if old_text in content:
    content = content.replace(old_text, new_text)
    print('Text replaced')
else:
    print('Text NOT replaced')

old_menu = '''        // --- LONG PRESS DIALOG FOR EDIT / DELETE / COPY ---
        if (showLongPressMenu && selectedMsgForMenu != null) {
            val msg = selectedMsgForMenu!!
            AlertDialog(
                onDismissRequest = { showLongPressMenu = false },
                title = {
                    Text("Message Options", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (msg.text.length > 50) msg.text.take(50) + "..." else msg.text.ifEmpty { "[Media Attachment]" },
                            color = subTextColor,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // 1. Copy Text
                        Surface(
                            color = if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                clipboardManager.setText(AnnotatedString(msg.text))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                showLongPressMenu = false
                            }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Copy Message Text", color = textColor, fontSize = 14.sp)
                            }
                        }

                        // 2. Edit Message
                        Surface(
                            color = if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                editingMsg = msg
                                editTextValue = msg.text
                                showLongPressMenu = false
                                showEditDialog = true
                            }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Edit Message", color = textColor, fontSize = 14.sp)
                            }
                        }

                        // 3. Delete Message
                        Surface(
                            color = Color(0x22EF4444),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.deleteChatMessage(msg.id)
                                Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                                showLongPressMenu = false
                            }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Delete Message", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLongPressMenu = false }) {
                        Text("Cancel", color = subTextColor)
                    }
                },
                containerColor = if (isDark) Color(0xFF1E1E2E) else Color.White
            )
        }'''

new_menu = '''        // --- LONG PRESS DIALOG FOR EDIT / DELETE / COPY / GEMINI ACTIONS ---
        if (showLongPressMenu && selectedMsgForMenu != null) {
            val msg = selectedMsgForMenu!!
            val formattedPreview = remember(msg.text) {
                com.example.util.MathFormatter.formatMathAndLatex(msg.text)
            }
            AlertDialog(
                onDismissRequest = { showLongPressMenu = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Message & Gemini Options", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (formattedPreview.length > 60) formattedPreview.take(60) + "..." else formattedPreview.ifEmpty { "[Media Attachment]" },
                            color = subTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))

                        // Gemini Action 1: Analyze Error
                        Surface(
                            color = Color(0x22F43F5E),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0x44F43F5E)),
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.generateAiErrorAnalysis(context, errorSubject, msg.text, msg.mediaUri, msg.mediaType)
                                Toast.makeText(context, "Gemini analyzing error...", Toast.LENGTH_SHORT).show()
                                showLongPressMenu = false
                            }
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFB7185), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("✨ Analyze Error with Gemini", color = Color(0xFFFB7185), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Get root cause, improvement plan & golden rule", color = subTextColor, fontSize = 10.sp)
                                }
                            }
                        }

                        // Gemini Action 2: Solve Doubt
                        Surface(
                            color = Color(0x226366F1),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0x446366F1)),
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.solveAiDoubt(context, errorSubject, msg.text, msg.mediaUri, msg.mediaType)
                                Toast.makeText(context, "Gemini solving doubt...", Toast.LENGTH_SHORT).show()
                                showLongPressMenu = false
                            }
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = Color(0xFFA5B4FC), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("🤖 Solve Doubt with Gemini", color = Color(0xFFA5B4FC), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Step-by-step solution, formula & NEET shortcut", color = subTextColor, fontSize = 10.sp)
                                }
                            }
                        }

                        // Gemini Action 3: Explain Message
                        Surface(
                            color = Color(0x2210B981),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0x4410B981)),
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.explainAiMessage(context, errorSubject, chatType, msg.text)
                                Toast.makeText(context, "Gemini explaining concept...", Toast.LENGTH_SHORT).show()
                                showLongPressMenu = false
                            }
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("🔎 Explain This Message in Detail", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Explain in simple Hinglish with step-by-step formulas", color = subTextColor, fontSize = 10.sp)
                                }
                            }
                        }

                        Divider(color = cardBorder, modifier = Modifier.padding(vertical = 2.dp))

                        // Copy Text
                        Surface(
                            color = if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                clipboardManager.setText(AnnotatedString(msg.text))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                showLongPressMenu = false
                            }
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Copy Message Text", color = textColor, fontSize = 13.sp)
                            }
                        }

                        // Edit Message
                        Surface(
                            color = if (isDark) Color(0x1AFFFFFF) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                editingMsg = msg
                                editTextValue = msg.text
                                showLongPressMenu = false
                                showEditDialog = true
                            }
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Edit Message", color = textColor, fontSize = 13.sp)
                            }
                        }

                        // Delete Message
                        Surface(
                            color = Color(0x22EF4444),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.deleteChatMessage(msg.id)
                                Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                                showLongPressMenu = false
                            }
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Delete Message", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLongPressMenu = false }) {
                        Text("Cancel", color = subTextColor)
                    }
                },
                containerColor = if (isDark) Color(0xFF1E1E2E) else Color.White
            )
        }'''

if old_menu in content:
    content = content.replace(old_menu, new_menu)
    print('Menu replaced')
else:
    print('Menu NOT replaced')

with open(p, 'w', encoding='utf-8') as f:
    f.write(content)
