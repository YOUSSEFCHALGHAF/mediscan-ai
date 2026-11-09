/** "Mozilla/5.0 ... Chrome/142.0 ... Windows NT 10.0" -> "Chrome 142 / Windows" (enregistré avec le token). */
export function decrireNavigateur(userAgent: string): string {
  const navigateurs: [RegExp, string][] = [
    [/Edg\/(\d+)/, 'Edge'],
    [/OPR\/(\d+)/, 'Opera'],
    [/Firefox\/(\d+)/, 'Firefox'],
    [/Chrome\/(\d+)/, 'Chrome'],
    [/Version\/(\d+).*Safari/, 'Safari'],
  ];
  const systemes: [RegExp, string][] = [
    [/Android/, 'Android'],
    [/iPhone|iPad/, 'iOS'],
    [/Windows/, 'Windows'],
    [/Mac OS X/, 'macOS'],
    [/Linux/, 'Linux'],
  ];
  const trouve = navigateurs.find(([motif]) => motif.test(userAgent));
  const nom = trouve ? `${trouve[1]} ${userAgent.match(trouve[0])?.[1]}` : 'Navigateur';
  const systeme = systemes.find(([motif]) => motif.test(userAgent))?.[1] ?? 'inconnu';
  return `${nom} / ${systeme}`.substring(0, 100);
}