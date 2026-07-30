export type FooterLinkItem = {
  label: string;
  href?: string;
};

export type FooterSection = {
  title: string;
  items: FooterLinkItem[];
};

export type SocialPlatform =
  | "facebook"
  | "youtube"
  | "tiktok"
  | "github"
  | "linkedin"
  | "discord";

export type SocialItem = {
  platform: SocialPlatform;
  label: string;
  href?: string;
};

export const footerBrand = {
  name: "E-Learning Platform",
  slogan: "Học tập linh hoạt, phát triển bền vững",
  contactItems: [
    { label: "Hotline", value: "Đang cập nhật" },
    { label: "Email", value: "Đang cập nhật" },
    { label: "Địa chỉ", value: "Đang cập nhật" },
  ],
};

export const footerSections: FooterSection[] = [
  {
    title: "Về chúng tôi",
    items: [
      { label: "Giới thiệu" },
      { label: "Liên hệ" },
      { label: "Điều khoản sử dụng" },
      { label: "Chính sách bảo mật" },
    ],
  },
  {
    title: "Hỗ trợ",
    items: [
      { label: "Trung tâm trợ giúp" },
      { label: "Hướng dẫn học tập" },
      { label: "Câu hỏi thường gặp" },
      { label: "Chính sách thanh toán" },
    ],
  },
  {
    title: "Tài nguyên",
    items: [
      { label: "Khóa học" },
      { label: "Blog" },
      { label: "Chứng chỉ" },
      { label: "AI Chatbot" },
    ],
  },
];

export const operatorInfo = {
  title: "Đơn vị vận hành",
  lines: [
    "Thông tin đơn vị vận hành đang được cập nhật",
    "Mã số doanh nghiệp: Đang cập nhật",
    "Thông tin pháp lý: Đang cập nhật",
  ],
};

export const socialItems: SocialItem[] = [
  { platform: "facebook", label: "Facebook" },
  { platform: "youtube", label: "YouTube" },
  { platform: "tiktok", label: "TikTok" },
  { platform: "github", label: "GitHub" },
  { platform: "linkedin", label: "LinkedIn" },
  { platform: "discord", label: "Discord" },
];
