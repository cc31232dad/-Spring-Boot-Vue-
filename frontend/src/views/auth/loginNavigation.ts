export function resolvePostLoginRoute(roles: string[], redirect?: string) {
  const fallback = roles.includes('ADMIN') ? '/admin' : roles.includes('FARMER') ? '/farmer' : '/'
  return redirect?.startsWith('/') && !redirect.startsWith('//') ? redirect : fallback
}
