import type { SVGProps } from "react";
import type { SocialPlatform } from "../config/footer-data";

type IconProps = SVGProps<SVGSVGElement>;

const commonProps = {
  viewBox: "0 0 24 24",
  width: 20,
  height: 20,
  fill: "currentColor",
  "aria-hidden": true,
  focusable: false,
} as const;

function FacebookIcon(props: IconProps) {
  return (
    <svg {...commonProps} {...props}>
      <path d="M13.6 21v-8h2.7l.4-3h-3.1V8.1c0-.9.3-1.5 1.6-1.5H17V3.9c-.3 0-1.4-.1-2.5-.1-2.5 0-4.2 1.5-4.2 4.4V10H7.5v3h2.8v8h3.3Z" />
    </svg>
  );
}

function YouTubeIcon(props: IconProps) {
  return (
    <svg {...commonProps} {...props}>
      <path d="M22 12c0-2.1-.2-3.6-.5-4.5-.3-.8-1-1.5-1.8-1.8C18.2 5.2 12 5.2 12 5.2s-6.2 0-7.7.5c-.8.3-1.5 1-1.8 1.8C2.2 8.4 2 9.9 2 12s.2 3.6.5 4.5c.3.8 1 1.5 1.8 1.8 1.5.5 7.7.5 7.7.5s6.2 0 7.7-.5c.8-.3 1.5-1 1.8-1.8.3-.9.5-2.4.5-4.5Zm-12 3.2V8.8l5.5 3.2-5.5 3.2Z" />
    </svg>
  );
}

function TikTokIcon(props: IconProps) {
  return (
    <svg {...commonProps} {...props}>
      <path d="M14.2 3h3c.2 1.8 1.2 3.2 2.8 4.1V10c-1.5 0-2.9-.5-4-1.4v6.6a5.8 5.8 0 1 1-5-5.7v3a2.8 2.8 0 1 0 2.2 2.7V3h1Z" />
    </svg>
  );
}

function GitHubIcon(props: IconProps) {
  return (
    <svg {...commonProps} {...props}>
      <path d="M12 2a10 10 0 0 0-3.2 19.5c.5.1.7-.2.7-.5v-1.9c-2.8.6-3.4-1.2-3.4-1.2-.5-1.1-1.1-1.4-1.1-1.4-.9-.6.1-.6.1-.6 1 0 1.6 1.1 1.6 1.1.9 1.6 2.4 1.1 2.9.9.1-.7.4-1.1.7-1.4-2.2-.3-4.6-1.1-4.6-5A3.9 3.9 0 0 1 6.8 8.7c-.1-.3-.5-1.3.1-2.8 0 0 .9-.3 2.8 1.1a9.8 9.8 0 0 1 5.1 0c1.9-1.4 2.8-1.1 2.8-1.1.6 1.5.2 2.5.1 2.8a3.9 3.9 0 0 1 1.1 2.8c0 3.9-2.4 4.7-4.6 5 .4.3.7 1 .7 2V21c0 .3.2.6.7.5A10 10 0 0 0 12 2Z" />
    </svg>
  );
}

function LinkedInIcon(props: IconProps) {
  return (
    <svg {...commonProps} {...props}>
      <path d="M5.3 7.8H2.1V22h3.2V7.8ZM3.7 2A1.9 1.9 0 1 0 3.7 5.8 1.9 1.9 0 0 0 3.7 2ZM21.9 13.9c0-4.3-2.3-6.3-5.4-6.3a4.7 4.7 0 0 0-4.2 2.3V7.8H9.1V22h3.2v-7c0-1.8.3-3.6 2.6-3.6 2.2 0 2.3 2.1 2.3 3.8V22h3.2v-8.1Z" />
    </svg>
  );
}

function DiscordIcon(props: IconProps) {
  return (
    <svg {...commonProps} {...props}>
      <path d="M19.5 5.3A16.5 16.5 0 0 0 15.4 4l-.5 1a15.4 15.4 0 0 0-5.8 0l-.5-1a16.4 16.4 0 0 0-4.1 1.3C1.9 9.2 1.2 13 1.5 16.8a16.8 16.8 0 0 0 5.1 2.6l1.2-1.7-1.8-.9.4-.3c3.5 1.6 7.7 1.6 11.2 0l.4.3-1.8.9 1.2 1.7a16.8 16.8 0 0 0 5.1-2.6c.4-4.4-.7-8.1-3-11.5ZM8.7 14.8c-1 0-1.9-1-1.9-2.2s.9-2.2 1.9-2.2c1.1 0 1.9 1 1.9 2.2s-.8 2.2-1.9 2.2Zm6.6 0c-1.1 0-1.9-1-1.9-2.2s.8-2.2 1.9-2.2c1 0 1.9 1 1.9 2.2s-.9 2.2-1.9 2.2Z" />
    </svg>
  );
}

export function SocialIcon({
  platform,
  ...props
}: IconProps & { platform: SocialPlatform }) {
  const icons: Record<SocialPlatform, (iconProps: IconProps) => JSX.Element> = {
    facebook: FacebookIcon,
    youtube: YouTubeIcon,
    tiktok: TikTokIcon,
    github: GitHubIcon,
    linkedin: LinkedInIcon,
    discord: DiscordIcon,
  };

  const Icon = icons[platform];
  return <Icon {...props} />;
}
