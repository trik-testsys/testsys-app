import React from 'react';
import { brandAssets } from '../../lib/brand-assets.mjs';

export function Footer({ brand = 'TestSys', links = [], linksLabel = 'Ссылки футтера' }) {
  return <footer className="ts-footer">
    <div className="ts-footer__inner">
      <div className="ts-footer__brand"><img className="ts-footer__logo" src={brandAssets.footer} alt={brand} /><span className="ts-footer__year">{new Date().getFullYear()}</span></div>
      {links.length ? <nav className="ts-footer__links" aria-label={linksLabel}>{links.map((link, index) => <a key={index} className="ts-footer__link" href={link.href}>{link.label}</a>)}</nav> : null}
    </div>
  </footer>;
}
