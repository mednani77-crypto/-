# prompt-master

A Claude Code mod that turns a rough prompt into a clearer one **inside the prompt box**, so you can read and edit it before you send it yourself. It never sends anything and never rewrites what you submit.

## Use

- **✦ Improve prompt** (band above the prompt box): rewrites your current draft with Haiku into four labelled parts — Role, Task, Context, Format (Arabic: الدور، المهمة، السياق، الشكل) — in the same language you wrote in, and puts it back in the box.
- **↩ Undo**: appears after an improvement and puts your original draft back.
- **`/improve <draft>`**: the same thing as a slash command, for surfaces that do not draw the band. With no text after it, it uses whatever is still in the box.

The rewrite keeps every fact you gave and invents nothing. If something important is missing, it ends with a line asking the assistant to first ask you up to 3 short questions.

An empty draft shows "Write your prompt first". If the model call fails, your draft is left as it was.

## Safety

There is no `prompt.submit` hook: what you send reaches Claude exactly as it is in the box.

## License

MIT
