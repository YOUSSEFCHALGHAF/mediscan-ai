import { messageErreurCamera } from './camera';

describe('messageErreurCamera', () => {
  it('explique un refus d\'autorisation', () => {
    expect(messageErreurCamera(new DOMException('refus', 'NotAllowedError'))).toContain('refusé');
  });

  it('explique une caméra déjà utilisée', () => {
    expect(messageErreurCamera(new DOMException('occupée', 'NotReadableError'))).toContain('déjà utilisée');
  });

  it('donne un message générique pour une erreur inconnue', () => {
    expect(messageErreurCamera('bizarre')).toContain('importer une photo');
  });
});