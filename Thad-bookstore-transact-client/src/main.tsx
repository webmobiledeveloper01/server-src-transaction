import ReactDOM from 'react-dom/client';
import AppComponent from './AppComponent';
import { CategoryProvider } from './contexts/CategoryContext';
import { CartProvider } from './contexts/CartContext';
import { OrderDetailsProvider } from './contexts/OrderDetailsContext';

const root = ReactDOM.createRoot(
  document.getElementById('root') as HTMLElement
);

root.render(
  // <React.StrictMode>
    <CartProvider>
      <CategoryProvider>
        <OrderDetailsProvider>
          <AppComponent/>
        </OrderDetailsProvider>
      </CategoryProvider>
    </CartProvider>
  // </React.StrictMode>
);

