export function thumbnailOf(url: string): string {
  return url.replace(/^(https:\/\/([a-z0-9-]+\.)?vgmtreasurechest\.com\/soundtracks\/[^/]+\/)([^/]+)$/, '$1thumbs/$3')
}
