import {
  footerBrand,
  footerSections,
  operatorInfo,
  socialItems,
  type FooterLinkItem,
} from "../config/footer-data";
import { SocialIcon } from "./social-icons";
import "../styles/Footer.css";

function FooterLink({ item }: { item: FooterLinkItem }) {
  if (!item.href) {
    return (
      <span className="app-footer__text-link app-footer__text-link--disabled">
        {item.label}
      </span>
    );
  }

  const isExternal = /^https?:\/\//i.test(item.href);

  return (
    <a
      className="app-footer__text-link"
      href={item.href}
      {...(isExternal
        ? { target: "_blank", rel: "noopener noreferrer" }
        : {})}
    >
      {item.label}
    </a>
  );
}

export function Footer() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="app-footer" aria-labelledby="app-footer-title">
      <div className="app-footer__container">
        <div className="app-footer__grid">
          <section className="app-footer__brand" aria-labelledby="app-footer-title">
            <div className="app-footer__brand-row">
              <div className="app-footer__logo" aria-hidden="true">
                EL
              </div>
              <div>
                <h2 id="app-footer-title" className="app-footer__brand-name">
                  {footerBrand.name}
                </h2>
                <p className="app-footer__slogan">{footerBrand.slogan}</p>
              </div>
            </div>

            <dl className="app-footer__contact-list">
              {footerBrand.contactItems.map((item) => (
                <div className="app-footer__contact-item" key={item.label}>
                  <dt>{item.label}:</dt>
                  <dd>{item.value}</dd>
                </div>
              ))}
            </dl>
          </section>

          {footerSections.map((section) => (
            <nav
              className="app-footer__section"
              aria-label={section.title}
              key={section.title}
            >
              <h3 className="app-footer__heading">{section.title}</h3>
              <ul className="app-footer__list">
                {section.items.map((item) => (
                  <li key={item.label}>
                    <FooterLink item={item} />
                  </li>
                ))}
              </ul>
            </nav>
          ))}

          <section className="app-footer__section app-footer__operator">
            <h3 className="app-footer__heading">{operatorInfo.title}</h3>
            {operatorInfo.lines.map((line) => (
              <p className="app-footer__operator-line" key={line}>
                {line}
              </p>
            ))}
          </section>
        </div>

        <div className="app-footer__bottom">
          <p className="app-footer__copyright">
            © {currentYear} {footerBrand.name}. All rights reserved.
          </p>

          <div className="app-footer__socials" aria-label="Mạng xã hội">
            {socialItems.map((item) =>
              item.href ? (
                <a
                  key={item.platform}
                  className="app-footer__social-link"
                  href={item.href}
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label={item.label}
                  title={item.label}
                >
                  <SocialIcon platform={item.platform} />
                </a>
              ) : (
                <span
                  key={item.platform}
                  className="app-footer__social-link app-footer__social-link--disabled"
                  aria-label={`${item.label} - Đang cập nhật`}
                  title={`${item.label} - Đang cập nhật`}
                  role="img"
                >
                  <SocialIcon platform={item.platform} />
                </span>
              ),
            )}
          </div>
        </div>
      </div>
    </footer>
  );
}

export default Footer;
