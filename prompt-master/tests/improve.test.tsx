import { expect, test } from 'claude-code/testing'

const BAND = {
  plugin: 'prompt-master',
  component: 'AbovePrompt',
  props: { hasSurvey: false, isCollapsed: false, isWorking: false, maxRows: 20, bodyColumns: 60, scroll: { offset: 0, bodyRows: 20 }, view: {} },
} as const

const USAGE = { input_tokens: 1, output_tokens: 1, cache_read_input_tokens: 0, cache_creation_input_tokens: 0 }

test('improves the draft into the box, then undo restores it; nothing is submitted', async ($, on) => {
  const toasts: string[] = []
  on('ui.toast', (_, e) => {
    toasts.push(String(e.text))
    return { value: undefined }
  })
  const fills: string[] = []
  let submitted = 0
  let box = 'رتب لي رحلة'
  on('prompt.read', () => ({ value: { text: box, cursor: box.length } }))
  on('model.complete', (_, e) => {
    expect(e.model).toBe('haiku')
    expect(e.prompt).toBe('رتب لي رحلة')
    return { value: { isAnswered: true, text: 'الدور: ...\nالمهمة: ...', usage: USAGE } }
  })
  on('prompt.fill', (_, e) => {
    fills.push(e.text)
    box = e.text
    return { isFilled: true, text: e.text, cursor: e.text.length }
  })
  on('prompt.submit', (_, e) => {
    submitted += 1
    return { drop: `test: ${e.text}` }
  })

  const ui = await $.ui.mount({ ...BAND, surface: 'terminal' })
  expect(await ui.find({ key: 'undo' })).toBeUndefined()
  await ui.press({ key: 'improve' })
  expect(fills).toEqual(['الدور: ...\nالمهمة: ...'])
  await ui.press({ key: 'undo' })
  expect(fills[1]).toBe('رتب لي رحلة')
  expect(submitted).toBe(0)
  expect(toasts).toContain('✦ Improved, review it then send')
  await ui.unmount()
})

test('an empty draft calls no model and fills nothing', async ($, on) => {
  const toasts: string[] = []
  on('ui.toast', (_, e) => {
    toasts.push(String(e.text))
    return { value: undefined }
  })
  let calls = 0
  on('prompt.read', () => ({ value: { text: '  ', cursor: 0 } }))
  on('model.complete', () => {
    calls += 1
    return { value: { isAnswered: false, reason: 'aborted', usage: USAGE } }
  })
  on('prompt.fill', () => {
    calls += 1
    return { isFilled: true, text: '', cursor: 0 }
  })
  const ui = await $.ui.mount({ ...BAND, surface: 'desktop' })
  await ui.press({ key: 'improve' })
  expect(calls).toBe(0)
  expect(toasts).toEqual(['Write your prompt first'])
  await ui.unmount()
})

test('a failed model call leaves the draft alone', async ($, on) => {
  const toasts: string[] = []
  on('ui.toast', (_, e) => {
    toasts.push(String(e.text))
    return { value: undefined }
  })
  let fills = 0
  on('prompt.read', () => ({ value: { text: 'hello', cursor: 5 } }))
  on('model.complete', () => {
    throw new Error('boom')
  })
  on('prompt.fill', () => {
    fills += 1
    return { isFilled: true, text: '', cursor: 0 }
  })
  const ui = await $.ui.mount({ ...BAND, surface: 'terminal' })
  await ui.press({ key: 'improve' })
  expect(fills).toBe(0)
  expect(await ui.find({ key: 'undo' })).toBeUndefined()
  await ui.unmount()
})
