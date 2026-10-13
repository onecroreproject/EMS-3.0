import { toast, Slide, Bounce, type ToastOptions, type Id } from 'react-toastify';

/**
 * Global EMS Notification Toast Helper
 * Supports:
 * - notify.success(message)
 * - notify.error(message)
 * - notify.info(message)
 * - notify.warn(message)
 * - notify.loading(message)
 * - notify.update(id, { render, type, autoClose })
 * - notify.promise(promise, { pending, success, error })
 */
export const notify = {
  success: (message: string, options?: ToastOptions): Id => {
    return toast.success(message, {
      transition: Slide,
      autoClose: 3500,
      ...options,
    });
  },

  error: (message: string, options?: ToastOptions): Id => {
    return toast.error(message, {
      transition: Slide,
      autoClose: 4000,
      ...options,
    });
  },

  delete: (message: string, options?: ToastOptions): Id => {
    return toast.error(message, {
      transition: Slide,
      autoClose: 3500,
      className: 'toast-delete',
      ...options,
    });
  },

  info: (message: string, options?: ToastOptions): Id => {
    return toast.info(message, {
      transition: Slide,
      autoClose: 3500,
      ...options,
    });
  },

  warn: (message: string, options?: ToastOptions): Id => {
    return toast.warn(message, {
      transition: Slide,
      autoClose: 3500,
      ...options,
    });
  },

  loading: (message: string, options?: ToastOptions): Id => {
    return toast.loading(message, {
      transition: Slide,
      ...options,
    });
  },

  update: (
    id: Id,
    optionsOrMessage: string | {
      render: string;
      type: 'success' | 'error' | 'delete' | 'info' | 'warning';
      autoClose?: number;
    },
    legacyType?: 'success' | 'error' | 'delete' | 'info' | 'warning',
    legacyAutoClose?: number
  ) => {
    let renderText = '';
    let incomingType: 'success' | 'error' | 'delete' | 'info' | 'warning' = 'success';
    let closeDuration = 3500;

    if (typeof optionsOrMessage === 'string') {
      renderText = optionsOrMessage;
      incomingType = legacyType || 'success';
      closeDuration = legacyAutoClose ?? 3500;
    } else {
      renderText = optionsOrMessage.render;
      incomingType = optionsOrMessage.type;
      closeDuration = optionsOrMessage.autoClose ?? 3500;
    }

    const isDelete = incomingType === 'delete';
    let toastType: 'success' | 'error' | 'info' | 'warning' = 'success';
    if (incomingType === 'delete' || incomingType === 'error') {
      toastType = 'error';
    } else if (incomingType === 'info') {
      toastType = 'info';
    } else if (incomingType === 'warning') {
      toastType = 'warning';
    } else {
      toastType = 'success';
    }

    toast.update(id, {
      render: renderText,
      type: toastType,
      isLoading: false,
      autoClose: closeDuration,
      closeButton: true,
      closeOnClick: true,
      transition: Slide,
      className: isDelete ? 'toast-delete' : undefined,
    });
  },

  promise: <T>(
    promise: Promise<T>,
    messages: {
      pending: string;
      success: string;
      error: string;
    },
    options?: ToastOptions
  ) => {
    return toast.promise(
      promise,
      {
        pending: messages.pending,
        success: messages.success,
        error: messages.error,
      },
      {
        transition: Slide,
        ...options,
      }
    );
  },

  dismiss: (id?: Id) => {
    toast.dismiss(id);
  },
};

export { toast, Slide, Bounce };
