package com.example.session

enum class AssistantState(val label: String, val quote: String) {
    IDLE(
        label = "Standing By",
        quote = "Just standing here looking gorgeous. Say 'Mir' or tap me, darling."
    ),
    LISTENING(
        label = "Listening",
        quote = "I'm all ears. Tell me what's on that brilliant mind of yours."
    ),
    THINKING(
        label = "Processing",
        quote = "Hold tight, computing pure genius..."
    ),
    SPEAKING(
        label = "Speaking",
        quote = "Listen closely, because I only say brilliant things once."
    ),
    EXECUTING_TOOL(
        label = "Executing",
        quote = "Handling device magic for you right now."
    ),
    ERROR(
        label = "Attention",
        quote = "Oops, something broke my groove. Let's try that again, darling."
    )
}
