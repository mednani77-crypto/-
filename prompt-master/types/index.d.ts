/** The draft as it was before the last improvement; null when there is nothing to undo. */
export type OriginalDraft = string | null

declare module 'claude-code' {
  interface PluginState {
    'prompt-master': {
      original: OriginalDraft
      /** True while the model is rewriting the draft. */
      isBusy: boolean
    }
  }
}
