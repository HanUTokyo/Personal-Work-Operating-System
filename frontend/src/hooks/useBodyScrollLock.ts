import { useEffect } from "react";

let lockCount = 0;
let scrollY = 0;
let originalBodyStyles: Pick<CSSStyleDeclaration, "position" | "top" | "left" | "right" | "width" | "overflow" | "paddingRight"> | null = null;

export function useBodyScrollLock() {
  useEffect(() => {
    lockCount += 1;

    if (lockCount === 1) {
      scrollY = window.scrollY;
      const scrollbarWidth = window.innerWidth - document.documentElement.clientWidth;
      originalBodyStyles = {
        position: document.body.style.position,
        top: document.body.style.top,
        left: document.body.style.left,
        right: document.body.style.right,
        width: document.body.style.width,
        overflow: document.body.style.overflow,
        paddingRight: document.body.style.paddingRight
      };

      document.body.style.position = "fixed";
      document.body.style.top = `-${scrollY}px`;
      document.body.style.left = "0";
      document.body.style.right = "0";
      document.body.style.width = "100%";
      document.body.style.overflow = "hidden";
      if (scrollbarWidth > 0) document.body.style.paddingRight = `${scrollbarWidth}px`;
    }

    return () => {
      lockCount = Math.max(0, lockCount - 1);
      if (lockCount > 0 || !originalBodyStyles) return;

      document.body.style.position = originalBodyStyles.position;
      document.body.style.top = originalBodyStyles.top;
      document.body.style.left = originalBodyStyles.left;
      document.body.style.right = originalBodyStyles.right;
      document.body.style.width = originalBodyStyles.width;
      document.body.style.overflow = originalBodyStyles.overflow;
      document.body.style.paddingRight = originalBodyStyles.paddingRight;
      originalBodyStyles = null;
      window.scrollTo(0, scrollY);
    };
  }, []);
}
