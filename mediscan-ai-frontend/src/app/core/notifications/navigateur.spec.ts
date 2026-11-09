import { decrireNavigateur } from './navigateur';

describe('decrireNavigateur', () => {
  it('reconnaît Chrome sous Windows', () => {
    expect(decrireNavigateur('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36'))
      .toBe('Chrome 142 / Windows');
  });

  it('reconnaît Edge (et non Chrome) et Firefox sous Android', () => {
    expect(decrireNavigateur('Mozilla/5.0 (Windows NT 10.0) Chrome/142.0.0.0 Safari/537.36 Edg/142.0.0.0')).toBe('Edge 142 / Windows');
    expect(decrireNavigateur('Mozilla/5.0 (Android 15; Mobile; rv:144.0) Gecko/144.0 Firefox/144.0')).toBe('Firefox 144 / Android');
  });
});