import { BrowserRouter, Route, Routes } from "react-router-dom";
import { Dashboard } from "./pages/Dashboard";
import { RiskDetail } from "./pages/RiskDetail";
import { RiskFormPage } from "./pages/RiskFormPage";

export default function App() {
  return (
    <BrowserRouter>
      <div className="container">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/risks/new" element={<RiskFormPage mode="new" />} />
          <Route path="/risks/:id" element={<RiskDetail />} />
          <Route path="/risks/:id/edit" element={<RiskFormPage mode="edit" />} />
        </Routes>
      </div>
    </BrowserRouter>
  );
}
