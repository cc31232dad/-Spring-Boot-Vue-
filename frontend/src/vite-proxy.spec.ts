import { describe, expect, it } from 'vitest'
import type { UserConfig } from 'vite'
import config from '../vite.config'

describe('Vite development proxy', () => {
  it('forwards API requests to the Spring Boot backend', () => {
    const userConfig = config as UserConfig

    expect(userConfig.server?.proxy?.['/api']).toMatchObject({
      target: 'http://localhost:8080',
      changeOrigin: true
    })
  })
})
