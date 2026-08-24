import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('workspace dashboard styles', () => {
  it('defines structured dashboard panels and responsive metric grids', () => {
    const styles = readFileSync(resolve(process.cwd(), 'src/styles.css'), 'utf8')

    for (const selector of [
      '.workspace-metrics',
      '.workspace-task-strip',
      '.workspace-panel',
      '.workspace-order-list',
      '.workspace-warning'
    ]) {
      expect(styles).toContain(selector)
    }
    expect(styles).toMatch(/\.workspace-metrics\s*\{[^}]*grid-template-columns:\s*repeat\(4,/s)
    expect(styles).toMatch(/@media \(max-width:\s*760px\)[\s\S]*\.workspace-metrics\s*\{[^}]*grid-template-columns:\s*repeat\(2,/)
  })
})
